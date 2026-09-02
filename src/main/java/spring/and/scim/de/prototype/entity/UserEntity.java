package spring.and.scim.de.prototype.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "scim_users")
@Setter
@Getter
public class UserEntity {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String userName;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String scimData;

    public UserEntity(String newId, String userName, String scimData) {
        this.id = newId;
        this.userName = userName;
        this.scimData = scimData;
    }

    public UserEntity() {
    }
}