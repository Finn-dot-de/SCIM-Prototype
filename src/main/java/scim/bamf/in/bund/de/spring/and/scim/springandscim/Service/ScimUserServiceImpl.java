package scim.bamf.in.bund.de.spring.and.scim.springandscim.Service;

import com.unboundid.scim2.common.types.Email;
import com.unboundid.scim2.common.types.Meta;
import com.unboundid.scim2.common.types.UserResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import scim.bamf.in.bund.de.spring.and.scim.springandscim.entity.UserEntity;
import scim.bamf.in.bund.de.spring.and.scim.springandscim.repository.UserRepository;

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
        String primaryEmail = extractPrimaryEmail(incomingUser);

        UserEntity entity = new UserEntity(newId, incomingUser.getUserName(), primaryEmail);
        userRepository.save(entity);

        incomingUser.setId(newId);
        incomingUser.setMeta(createScimMeta(newId));

        return incomingUser;
    }

    @Override
    public Optional<UserResource> getUser(String id) {
        return userRepository.findById(id)
                .map(this::mapToUserResource);
    }

    private String extractPrimaryEmail(UserResource user) {
        if (user.getEmails() != null && !user.getEmails().isEmpty()) {
            return user.getEmails().getFirst().getValue();
        }
        return null;
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
        UserResource scimUser = new UserResource();
        scimUser.setId(dbUser.getId());
        scimUser.setUserName(dbUser.getUserName());

        if (dbUser.getPrimaryEmail() != null) {
            scimUser.setEmails(new Email()
                    .setValue(dbUser.getPrimaryEmail())
                    .setType("work")
                    .setPrimary(true));
        }
        return scimUser;
    }
}
