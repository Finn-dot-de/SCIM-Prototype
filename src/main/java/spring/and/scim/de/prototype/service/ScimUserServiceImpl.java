package spring.and.scim.de.prototype.service;

import com.unboundid.scim2.common.exceptions.BadRequestException;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.messages.PatchOperation;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.Meta;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.utils.FilterEvaluator;
import com.unboundid.scim2.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.and.scim.de.prototype.entity.UserEntity;
import spring.and.scim.de.prototype.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public ListResponse<UserResource> searchUsers(String filterString, int startIndex, int count) {

        List<UserEntity> allDbUsers = userRepository.findAll();
        List<UserResource> matchedUsers;

        if (filterString == null || filterString.isBlank()) {
            matchedUsers = allDbUsers.stream()
                    .map(this::mapToUserResource)
                    .collect(Collectors.toList());
        } else {
            try {
                Filter scimFilter = Filter.fromString(filterString);
                FilterEvaluator evaluator = new FilterEvaluator();

                matchedUsers = allDbUsers.stream()
                        .filter(dbUser -> {
                            try {
                                JsonNode userNode = JsonUtils.getObjectReader().readTree(dbUser.getScimData());

                                return scimFilter.visit(evaluator, userNode);
                            } catch (ScimException e) {
                                return false;
                            }
                        })
                        .map(this::mapToUserResource)
                        .collect(Collectors.toList());

            } catch (BadRequestException e) {
                throw new IllegalStateException("Ungültiger SCIM-Filter: " + e.getMessage());
            }
        }

        int fromIndex = Math.max(0, startIndex - 1);
        int toIndex = Math.min(matchedUsers.size(), fromIndex + count);

        List<UserResource> pagedResults = (fromIndex <= matchedUsers.size())
                ? matchedUsers.subList(fromIndex, toIndex)
                : List.of();

        return new ListResponse<>(
                matchedUsers.size(),
                pagedResults,
                startIndex,
                count
        );
    }

    @Override
    public Optional<UserResource> patchUser(String id, PatchRequest patchRequest) {
        return userRepository.findById(id).map(dbUser -> {
            try {
                ObjectNode node = (ObjectNode) JsonUtils.getObjectReader()
                        .readTree(dbUser.getScimData());

                for (PatchOperation op : patchRequest.getOperations()) {
                    op.apply(node);
                }

                UserResource scimUser = JsonUtils.nodeToValue(node, UserResource.class);
                scimUser.getMeta().setLastModified(Calendar.getInstance());

                dbUser.setScimData(JsonUtils.getObjectWriter().writeValueAsString(scimUser));
                userRepository.save(dbUser);
                return scimUser;
            } catch (ScimException e) {
                throw new IllegalArgumentException("Ungültiger Patch: " + e.getMessage(), e);
            }
        });
    }

}

