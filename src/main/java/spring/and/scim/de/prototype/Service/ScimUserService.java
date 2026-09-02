package spring.and.scim.de.prototype.Service;

import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.UserResource;

import java.util.Optional;

public interface ScimUserService {

    UserResource createUser(UserResource incomingUser);

    Optional<UserResource> getUser(String id);

    Optional<UserResource> patchUser(String email, PatchRequest patchRequest);
}
