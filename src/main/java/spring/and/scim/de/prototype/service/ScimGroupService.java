package spring.and.scim.de.prototype.service;

import com.unboundid.scim2.common.types.GroupResource;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import spring.and.scim.de.prototype.entity.GroupEntity;
import spring.and.scim.de.prototype.repository.GroupRepository;
import spring.and.scim.de.prototype.scim.ScimMetaFactory;
import spring.and.scim.de.prototype.scim.ScimResourceType;

/**
 * SCIM-/Groups-Service. Die gesamte Ablauflogik steckt in
 * {@link ScimResourceService} — hier steht nur, was die Gruppe ausmacht.
 */
@Service
public class ScimGroupService extends ScimResourceService<GroupResource, GroupEntity> {

    public ScimGroupService(GroupRepository repository, ScimMetaFactory metaFactory) {
        super(repository, metaFactory);
    }

    @Override
    protected ScimResourceType type() {
        return ScimResourceType.GROUP;
    }

    @Override
    protected Class<GroupResource> resourceClass() {
        return GroupResource.class;
    }

    @Override
    protected GroupEntity newEntity() {
        return new GroupEntity();
    }

    @Override
    protected @Nullable String uniqueAttributeOf(GroupResource resource) {
        return resource.getDisplayName();
    }
}
