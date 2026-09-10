package spring.and.scim.de.prototype.repository;

import org.springframework.stereotype.Repository;
import spring.and.scim.de.prototype.entity.GroupEntity;

@Repository
public interface GroupRepository extends ScimEntityRepository<GroupEntity> {
}
