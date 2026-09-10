package spring.and.scim.de.prototype.scim;

import com.unboundid.scim2.common.types.Meta;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Baut die {@code meta}-Struktur einer SCIM-Ressource.
 * <p>
 * Die Location wird aus dem laufenden Request abgeleitet statt konfiguriert,
 * damit sie auch hinter einem Proxy oder auf einem anderen Port stimmt. Die
 * Uhr ist injiziert, damit Zeitstempel ohne laufende Anwendung testbar sind.
 */
@Component
public class ScimMetaFactory {

    private final Clock clock;

    public ScimMetaFactory(Clock clock) {
        this.clock = clock;
    }

    /** Meta einer neu angelegten Ressource: {@code created == lastModified}. */
    public Meta created(ScimResourceType type, String id) {
        Calendar now = now();
        return meta(type, id, now, now);
    }

    /**
     * Meta einer geaenderten Ressource. {@code created} wird aus dem bisherigen
     * Stand uebernommen, damit PATCH den Anlagezeitpunkt nicht verliert.
     */
    public Meta modified(ScimResourceType type, String id, @Nullable Meta previous) {
        return meta(type, id, createdOf(previous), now());
    }

    private Meta meta(ScimResourceType type, String id, Calendar created, Calendar lastModified) {
        Meta meta = new Meta();
        meta.setResourceType(type.typeName());
        meta.setLocation(locationOf(type, id));
        meta.setCreated(created);
        meta.setLastModified(lastModified);
        return meta;
    }

    private URI locationOf(ScimResourceType type, String id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(type.path())
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    private Calendar createdOf(@Nullable Meta previous) {
        if (previous == null || previous.getCreated() == null) {
            return now();
        }
        return previous.getCreated();
    }

    private Calendar now() {
        return GregorianCalendar.from(ZonedDateTime.now(clock));
    }
}
