package spring.and.scim.de.prototype.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * SCIM-User. Traegt nur noch die Schema-Abweichung gegenueber
 * {@link ScimEntity}: der Eindeutigkeitsschluessel ist der {@code userName}.
 */
@Entity
@Table(name = "scim_users")
@AttributeOverride(
        name = "businessKey",
        column = @Column(name = "user_name", nullable = false, unique = true))
public class UserEntity extends ScimEntity {
}
