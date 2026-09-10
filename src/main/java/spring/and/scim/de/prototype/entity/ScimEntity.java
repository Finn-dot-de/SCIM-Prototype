package spring.and.scim.de.prototype.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Gemeinsames Schema aller SCIM-Ressourcen: technische ID, fachlicher
 * Eindeutigkeitsschluessel und die vollstaendige SCIM-Ressource als JSONB.
 * <p>
 * {@code businessKey} heisst in der Datenbank je nach Ressource
 * {@code user_name} oder {@code display_name}; die Unterklassen legen das per
 * {@code @AttributeOverride} fest. So kennt der Code nur <em>einen</em> Namen
 * und kann generisch darauf zugreifen, ohne dass sich das Schema aendert.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class ScimEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String businessKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String scimData;
}
