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
@Table(name = "scim_groups")
@Setter
@Getter
public class GroupEntity {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String displayName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String scimData;

    public GroupEntity(String id, String displayName, String scimData) {
        this.id = id;
        this.displayName = displayName;
        this.scimData = scimData;
    }

    public GroupEntity() {}
}
