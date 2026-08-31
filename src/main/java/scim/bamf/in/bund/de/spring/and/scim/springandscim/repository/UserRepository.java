package scim.bamf.in.bund.de.spring.and.scim.springandscim.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import scim.bamf.in.bund.de.spring.and.scim.springandscim.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
}
