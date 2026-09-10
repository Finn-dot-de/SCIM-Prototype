package spring.and.scim.de.prototype.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * SCIM-Gruppe. Traegt nur noch die Schema-Abweichung gegenueber
 * {@link ScimEntity}: der Eindeutigkeitsschluessel ist der {@code displayName}.
 */
@Entity
@Table(name = "scim_groups")
@AttributeOverride(
        name = "businessKey",
        column = @Column(name = "display_name", nullable = false, unique = true))
public class GroupEntity extends ScimEntity {
}
