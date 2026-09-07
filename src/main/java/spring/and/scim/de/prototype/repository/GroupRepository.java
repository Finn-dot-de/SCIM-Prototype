package spring.and.scim.de.prototype.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.and.scim.de.prototype.entity.GroupEntity;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, String> {
}
