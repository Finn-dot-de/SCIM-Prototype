package spring.and.scim.de.prototype.repository;

import org.springframework.stereotype.Repository;
import spring.and.scim.de.prototype.entity.UserEntity;

@Repository
public interface UserRepository extends ScimEntityRepository<UserEntity> {
}
