package spring.and.scim.de.prototype.service;

import com.unboundid.scim2.common.exceptions.BadRequestException;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.messages.PatchOperation;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.Meta;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.utils.FilterEvaluator;
import com.unboundid.scim2.common.utils.JsonUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.and.scim.de.prototype.advise.UserNotFoundException;
import spring.and.scim.de.prototype.entity.GroupEntity;
import spring.and.scim.de.prototype.repository.GroupRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScimGroupServiceImpl implements ScimGroupService {

    private static final String BASE_URL = "http://localhost:8080/scim/v2/Groups/";

    private final GroupRepository groupRepository;

    public ScimGroupServiceImpl(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Override
    public GroupResource createGroup(GroupResource incomingGroup) {
        log.info("Erstelle SCIM Gruppe: {}", incomingGroup.getDisplayName());

        String newId = UUID.randomUUID().toString();
        incomingGroup.setId(newId);
        incomingGroup.setMeta(createMeta(newId));

        persist(new GroupEntity(newId, incomingGroup.getDisplayName(), null), incomingGroup);
        return incomingGroup;
    }

    @Override
    public Optional<GroupResource> getGroup(String id) {
        return groupRepository.findById(id).map(this::mapToGroupResource);
    }

    @Override
    public Optional<GroupResource> patchGroup(String groupId, PatchRequest patchRequest) {
        log.info("Verarbeite PATCH-Request für Gruppe ID: {}", groupId);

        return groupRepository.findById(groupId).map(dbGroup -> {
            try {
                ObjectNode node = (ObjectNode) JsonUtils.getObjectReader()
                        .readTree(dbGroup.getScimData());

                for (PatchOperation op : patchRequest.getOperations()) {
                    log.debug("Patch Operation: {} auf Pfad: {}", op.getOpType(), op.getPath());
                    op.apply(node);
                }

                GroupResource group = JsonUtils.getObjectReader()
                        .forType(GroupResource.class)
                        .readValue(node);

                group.getMeta().setLastModified(Calendar.getInstance());
                dbGroup.setDisplayName(group.getDisplayName());

                persist(dbGroup, group);
                return group;

            } catch (ScimException e) {
                throw new IllegalArgumentException("Ungültiger Patch: " + e.getMessage(), e);
            }
        });
    }

    @Transactional
    @Override
    public void deleteScimGroup(String id) {
        log.info("Delete SCIM Group with id: {}", id);

        Pattern UUID_REGEX =
                Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

        boolean isValidUuid = UUID_REGEX.matcher(id).matches();
        log.info("Is valid UUID? >>>>>>>>>>>>>>>>>>>>>>>>>>> {}", isValidUuid);

        if (!isValidUuid) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400) ,"User with id: " + id + " stimmt mit dem Format nicht überein");
        }

        if (groupRepository.existsById(id)) {
            groupRepository.deleteById(id);
        } else {
            throw new UserNotFoundException("User with id: " + id + " not found");
        }
    }

    @Override
    public ListResponse<GroupResource> searchScimGroups(String filterString, int startIndex, int count) {

        List<GroupEntity> allDbGroups = groupRepository.findAll();
        List<GroupResource> matchedGroups;

        if (filterString == null || filterString.isBlank()) {
            matchedGroups = allDbGroups.stream()
                    .map(this::mapToGroupResource)
                    .collect(Collectors.toList());
        } else {
            try {
                Filter scimFilter = Filter.fromString(filterString);
                FilterEvaluator evaluator = new FilterEvaluator();

                matchedGroups = allDbGroups.stream()
                        .filter(dbUser -> {
                            try {
                                JsonNode userNode = JsonUtils.getObjectReader().readTree(dbUser.getScimData());

                                return scimFilter.visit(evaluator, userNode);
                            } catch (ScimException e) {
                                return false;
                            }
                        })
                        .map(this::mapToGroupResource)
                        .collect(Collectors.toList());

            } catch (BadRequestException e) {
                throw new IllegalStateException("Ungültiger SCIM-Filter: " + e.getMessage());
            }
        }

        int fromIndex = Math.max(0, startIndex - 1);
        int toIndex = Math.min(matchedGroups.size(), fromIndex + count);

        List<GroupResource> pagedResults = (fromIndex <= matchedGroups.size())
                ? matchedGroups.subList(fromIndex, toIndex)
                : Collections.emptyList();

        return new ListResponse<>(
                matchedGroups.size(),
                pagedResults,
                startIndex,
                count
        );
    }  
    

    private GroupResource mapToGroupResource(GroupEntity dbGroup) {
        return JsonUtils.getObjectReader()
                .forType(GroupResource.class)
                .readValue(dbGroup.getScimData());
    }

    private void persist(GroupEntity entity, GroupResource group) {
        entity.setScimData(JsonUtils.getObjectWriter().writeValueAsString(group));
        groupRepository.save(entity);
    }

    private Meta createMeta(String id) {
        Meta meta = new Meta();
        meta.setResourceType("Group");
        meta.setCreated(Calendar.getInstance());
        meta.setLastModified(Calendar.getInstance());
        meta.setLocation(URI.create(BASE_URL + id));
        return meta;
    }
}
