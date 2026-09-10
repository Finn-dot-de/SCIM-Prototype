package spring.and.scim.de.prototype.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.and.scim.de.prototype.entity.ScimEntity;

import java.util.Optional;

/**
 * Gemeinsame Repository-Schnittstelle aller SCIM-Ressourcen. Der generische
 * Service arbeitet ausschliesslich gegen diesen Typ und braucht daher keine
 * Kenntnis von {@code UserEntity} oder {@code GroupEntity}.
 *
 * @param <E> konkrete Entity
 */
@NoRepositoryBean
public interface ScimEntityRepository<E extends ScimEntity> extends JpaRepository<E, String> {

    /**
     * Sucht ueber den fachlichen Schluessel ({@code userName} bzw.
     * {@code displayName}) — Grundlage fuer den Duplikat-Check, den IdPs wie
     * Okta oder Entra vor dem Anlegen erwarten.
     */
    Optional<E> findByBusinessKey(String businessKey);
}
