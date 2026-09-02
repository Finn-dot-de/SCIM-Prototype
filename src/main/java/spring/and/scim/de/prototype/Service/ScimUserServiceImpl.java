package spring.and.scim.de.prototype.Service;

import com.unboundid.scim2.common.messages.PatchOpType;
import com.unboundid.scim2.common.messages.PatchOperation;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.Meta;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.and.scim.de.prototype.entity.UserEntity;
import spring.and.scim.de.prototype.repository.UserRepository;

import java.net.URI;
import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ScimUserServiceImpl implements ScimUserService {

    private final UserRepository userRepository;

    public ScimUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResource createUser(UserResource incomingUser) {
        log.info("Verarbeite SCIM User-Erstellung für: {}", incomingUser.getUserName());

        String newId = UUID.randomUUID().toString();
        incomingUser.setId(newId);
        incomingUser.setMeta(createScimMeta(newId));

        String scimJson = JsonUtils.getObjectWriter().writeValueAsString(incomingUser);

        UserEntity entity = new UserEntity(newId, incomingUser.getUserName(), scimJson);
        userRepository.save(entity);

        return incomingUser;
    }

    @Override
    public Optional<UserResource> getUser(String id) {
        return userRepository.findById(id)
                .map(this::mapToUserResource);
    }

    private Meta createScimMeta(String id) {
        Meta meta = new Meta();
        meta.setResourceType("User");
        meta.setCreated(Calendar.getInstance());
        meta.setLastModified(Calendar.getInstance());
        meta.setLocation(URI.create("http://localhost:8080/scim/v2/Users/" + id));
        return meta;
    }

    private UserResource mapToUserResource(UserEntity dbUser) {
        return JsonUtils.getObjectReader()
                .forType(UserResource.class)
                .readValue(dbUser.getScimData());
    }

    @Override
    public Optional<UserResource> patchUser(String id, PatchRequest patchRequest) {
        return userRepository.findById(id).map(dbUser -> {

            UserResource scimUser = mapToUserResource(dbUser);

            for (PatchOperation op : patchRequest.getOperations()) {
                log.info("Patch Operation: {} auf Pfad: {}", op.getOpType(), op.getPath());

                String pathString = op.getPath().toString();

                if (op.getOpType() == PatchOpType.REPLACE && pathString.equals("active")) {
                    boolean isActive = op.getJsonNode().asBoolean();
                    scimUser.setActive(isActive);
                }

                if (op.getOpType() == PatchOpType.REPLACE && pathString.equals("name.givenName")) {
                    String newGivenName = op.getJsonNode().asString();
                    if (scimUser.getName() == null) {
                        scimUser.setName(new com.unboundid.scim2.common.types.Name());
                    }
                    scimUser.getName().setGivenName(newGivenName);
                }

            }

            scimUser.getMeta().setLastModified(Calendar.getInstance());

            try {
                String updatedJson = JsonUtils.getObjectWriter().writeValueAsString(scimUser);

                dbUser.setScimData(updatedJson);
                userRepository.save(dbUser);

            } catch (Exception e) {
                log.error("Fehler beim Speichern des gepatchten Users", e);
                throw new RuntimeException("Patch fehlgeschlagen", e);
            }

            return scimUser;
        });
    }
}