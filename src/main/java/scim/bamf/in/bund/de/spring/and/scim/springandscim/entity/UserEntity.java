package scim.bamf.in.bund.de.spring.and.scim.springandscim.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scim_users")
@Setter
@Getter
public class UserEntity {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String userName;

    private String primaryEmail;

    public UserEntity(String newId, String userName, String primaryEmail) {
        this.id = newId;
        this.userName = userName;
        this.primaryEmail = primaryEmail;
    }

    public UserEntity() {

    }
}
