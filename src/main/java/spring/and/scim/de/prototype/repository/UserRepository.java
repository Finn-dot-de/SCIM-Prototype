package spring.and.scim.de.prototype.repository;

import com.unboundid.scim2.common.types.UserResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.and.scim.de.prototype.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    UserResource deleteUserEntityById(String id);
}
