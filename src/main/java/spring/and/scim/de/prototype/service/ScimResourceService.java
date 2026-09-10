package spring.and.scim.de.prototype.service;

import com.unboundid.scim2.common.BaseScimResource;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.Meta;
import com.unboundid.scim2.common.utils.FilterEvaluator;
import com.unboundid.scim2.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import spring.and.scim.de.prototype.entity.ScimEntity;
import spring.and.scim.de.prototype.repository.ScimEntityRepository;
import spring.and.scim.de.prototype.scim.ScimErrorException;
import spring.and.scim.de.prototype.scim.ScimMetaFactory;
import spring.and.scim.de.prototype.scim.ScimPagination;
import spring.and.scim.de.prototype.scim.ScimResourceType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Der komplette SCIM-Lebenszyklus einer Ressource — anlegen, lesen, suchen,
 * patchen, loeschen.
 * <p>
 * Template Method: der Ablauf steht hier, ressourcenspezifisch sind
 * ausschliesslich die vier {@code protected}-Haken am Ende der Klasse.
 *
 * @param <R> SCIM-Ressourcentyp, z.B. {@code UserResource}
 * @param <E> zugehoerige Entity, z.B. {@code UserEntity}
 */
@Slf4j
public abstract class ScimResourceService<R extends BaseScimResource, E extends ScimEntity> {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final ScimEntityRepository<E> repository;
    private final ScimMetaFactory metaFactory;

    protected ScimResourceService(ScimEntityRepository<E> repository, ScimMetaFactory metaFactory) {
        this.repository = repository;
        this.metaFactory = metaFactory;
    }

    // ------------------------------------------------------------------
    // Anwendungsfaelle
    // ------------------------------------------------------------------

    @Transactional
    public R create(R resource) {
        String uniqueValue = requireUniqueAttribute(resource);
        requireAvailable(uniqueValue, null);

        String id = UUID.randomUUID().toString();
        resource.setId(id);
        resource.setMeta(metaFactory.created(type(), id));

        log.info("Lege {} an: {}", type().typeName(), uniqueValue);
        return store(newEntity(), resource, id, uniqueValue);
    }

    @Transactional(readOnly = true)
    public R findById(String id) {
        return toResource(findEntity(id));
    }

    @Transactional(readOnly = true)
    public ListResponse<R> search(@Nullable String filterExpression, int startIndex, int count) {
        log.debug("Suche {}. Filter: '{}', Start: {}, Count: {}",
                type().typeName(), filterExpression, startIndex, count);

        return ScimPagination.paginate(findMatching(filterExpression), startIndex, count);
    }

    @Transactional
    public R patch(String id, PatchRequest patchRequest) {
        E entity = findEntity(id);
        R current = toResource(entity);
        Meta previousMeta = current.getMeta();

        R patched = applyPatch(current, patchRequest);

        // Ein Patch darf weder die ID noch den Anlagezeitpunkt ueberschreiben.
        patched.setId(id);
        patched.setMeta(metaFactory.modified(type(), id, previousMeta));

        String uniqueValue = requireUniqueAttribute(patched);
        requireAvailable(uniqueValue, id);

        log.info("Patche {} {}", type().typeName(), id);
        return store(entity, patched, id, uniqueValue);
    }

    @Transactional
    public void delete(String id) {
        requireValidId(id);
        if (!repository.existsById(id)) {
            throw ScimErrorException.notFound(type(), id);
        }
        repository.deleteById(id);
        log.info("{} {} geloescht", type().typeName(), id);
    }

    // ------------------------------------------------------------------
    // Suche und Filter
    // ------------------------------------------------------------------

    private List<R> findMatching(@Nullable String filterExpression) {
        List<E> all = repository.findAll();
        if (!StringUtils.hasText(filterExpression)) {
            return all.stream().map(this::toResource).toList();
        }

        Filter filter = parseFilter(filterExpression);
        return all.stream()
                .filter(entity -> matches(filter, entity))
                .map(this::toResource)
                .toList();
    }

    private Filter parseFilter(String filterExpression) {
        try {
            return Filter.fromString(filterExpression);
        } catch (ScimException e) {
            // Das SDK liefert hier bereits 400 mit scimType "invalidFilter".
            throw ScimErrorException.from(e);
        }
    }

    /**
     * Ein nicht auswertbarer Filter ist ein Fehler des Aufrufers. Die Exception
     * wird deshalb gemeldet und nicht als "Datensatz passt nicht" behandelt —
     * sonst antwortet der Server auf einen kaputten Filter mit einer leeren
     * Trefferliste statt mit einem Fehler.
     */
    private boolean matches(Filter filter, E entity) {
        try {
            return FilterEvaluator.evaluate(filter, readTree(entity));
        } catch (ScimException e) {
            throw ScimErrorException.from(e);
        }
    }

    // ------------------------------------------------------------------
    // Persistenz und Mapping
    // ------------------------------------------------------------------

    private E findEntity(String id) {
        requireValidId(id);
        return repository.findById(id)
                .orElseThrow(() -> ScimErrorException.notFound(type(), id));
    }

    private R store(E entity, R resource, String id, String uniqueValue) {
        entity.setId(id);
        entity.setBusinessKey(uniqueValue);
        entity.setScimData(JsonUtils.getObjectWriter().writeValueAsString(resource));
        repository.save(entity);
        return resource;
    }

    private R toResource(E entity) {
        return JsonUtils.getObjectReader()
                .forType(resourceClass())
                .readValue(entity.getScimData());
    }

    private JsonNode readTree(E entity) {
        return JsonUtils.getObjectReader().readTree(entity.getScimData());
    }

    private R applyPatch(R resource, PatchRequest patchRequest) {
        try {
            return patchRequest.applyToResource(resource);
        } catch (ScimException e) {
            throw ScimErrorException.from(e);
        }
    }

    // ------------------------------------------------------------------
    // Validierung
    // ------------------------------------------------------------------

    private void requireValidId(@Nullable String id) {
        if (id == null || !UUID_PATTERN.matcher(id).matches()) {
            throw ScimErrorException.badRequest("invalidValue",
                    "'%s' ist keine gueltige %s-ID, erwartet wird eine UUID."
                            .formatted(id, type().typeName()));
        }
    }

    private String requireUniqueAttribute(R resource) {
        String value = uniqueAttributeOf(resource);
        if (!StringUtils.hasText(value)) {
            throw ScimErrorException.badRequest("invalidValue",
                    "%s ist ein Pflichtfeld.".formatted(type().uniqueAttribute()));
        }
        return value;
    }

    /**
     * Stellt sicher, dass der fachliche Schluessel frei ist. Ohne die Pruefung
     * greift erst die Unique-Constraint der Datenbank, und der Client bekaeme
     * einen 500er statt des von SCIM vorgesehenen 409.
     *
     * @param ownId ID der Ressource, die den Wert behalten darf ({@code null}
     *              beim Anlegen)
     */
    private void requireAvailable(String uniqueValue, @Nullable String ownId) {
        Optional<E> owner = repository.findByBusinessKey(uniqueValue);
        if (owner.isPresent() && !owner.get().getId().equals(ownId)) {
            throw ScimErrorException.conflict(type(), uniqueValue);
        }
    }

    // ------------------------------------------------------------------
    // Haken der konkreten Ressource
    // ------------------------------------------------------------------

    /** Ressourcenart samt Pfad und Name des Eindeutigkeitsattributs. */
    protected abstract ScimResourceType type();

    /** Zieltyp fuer die Deserialisierung des gespeicherten SCIM-JSON. */
    protected abstract Class<R> resourceClass();

    /** Leere Entity fuer einen Neuanlage-Vorgang. */
    protected abstract E newEntity();

    /** {@code userName} beim User, {@code displayName} bei der Gruppe. */
    protected abstract @Nullable String uniqueAttributeOf(R resource);
}
