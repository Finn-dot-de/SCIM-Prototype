package spring.and.scim.de.prototype.service;

import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.GroupResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ScimGroupService {

    Optional<GroupResource> getGroup(String groupId);

    GroupResource createGroup(GroupResource group);

    @Transactional
    Optional<GroupResource> patchGroup(String groupId, PatchRequest patchRequest);

    void updateUserWithNewGroup(String userId, GroupResource scimGroup, boolean remove);
}
