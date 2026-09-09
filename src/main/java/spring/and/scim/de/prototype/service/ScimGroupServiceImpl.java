package spring.and.scim.de.prototype.service;

import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.messages.PatchOperation;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.Meta;
import com.unboundid.scim2.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.and.scim.de.prototype.entity.GroupEntity;
import spring.and.scim.de.prototype.repository.GroupRepository;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;

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
