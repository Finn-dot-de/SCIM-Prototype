package scim.bamf.in.bund.de.spring.and.scim.springandscim.Service;

import com.unboundid.scim2.common.types.UserResource;

import java.util.Optional;

public interface ScimUserService {

    UserResource createUser(UserResource incomingUser);

    Optional<UserResource> getUser(String id);
}
