package spring.and.scim.de.prototype.service;

import com.unboundid.scim2.common.types.UserResource;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import spring.and.scim.de.prototype.entity.UserEntity;
import spring.and.scim.de.prototype.repository.UserRepository;
import spring.and.scim.de.prototype.scim.ScimMetaFactory;
import spring.and.scim.de.prototype.scim.ScimResourceType;

/**
 * SCIM-/Users-Service. Die gesamte Ablauflogik steckt in
 * {@link ScimResourceService} — hier steht nur, was den User ausmacht.
 */
@Service
public class ScimUserService extends ScimResourceService<UserResource, UserEntity> {

    public ScimUserService(UserRepository repository, ScimMetaFactory metaFactory) {
        super(repository, metaFactory);
    }

    @Override
    protected ScimResourceType type() {
        return ScimResourceType.USER;
    }

    @Override
    protected Class<UserResource> resourceClass() {
        return UserResource.class;
    }

    @Override
    protected UserEntity newEntity() {
        return new UserEntity();
    }

    @Override
    protected @Nullable String uniqueAttributeOf(UserResource resource) {
        return resource.getUserName();
    }
}
