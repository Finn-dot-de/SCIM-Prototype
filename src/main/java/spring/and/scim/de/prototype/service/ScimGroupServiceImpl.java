package spring.and.scim.de.prototype.service;

import com.unboundid.scim2.common.messages.PatchOpType;
import com.unboundid.scim2.common.messages.PatchOperation;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.Member;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.and.scim.de.prototype.entity.GroupEntity;
import spring.and.scim.de.prototype.repository.GroupRepository;
import spring.and.scim.de.prototype.repository.UserRepository;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ScimGroupServiceImpl implements ScimGroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public ScimGroupServiceImpl(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Override
    public GroupResource createGroup(GroupResource incomingGroup) {
        log.info("Erstelle SCIM Gruppe: {}", incomingGroup.getDisplayName());

        String newId = UUID.randomUUID().toString();
        incomingGroup.setId(newId);

        com.unboundid.scim2.common.types.Meta meta = new com.unboundid.scim2.common.types.Meta();
        meta.setResourceType("Group");
        meta.setCreated(Calendar.getInstance());
        meta.setLastModified(Calendar.getInstance());
        meta.setLocation(URI.create("http://localhost:8080/scim/v2/Groups/" + newId));
        incomingGroup.setMeta(meta);

        try {
            String scimJson = JsonUtils.getObjectWriter().writeValueAsString(incomingGroup);
            GroupEntity entity = new GroupEntity(newId, incomingGroup.getDisplayName(), scimJson);
            groupRepository.save(entity);
        } catch (Exception e) {
            log.error("Fehler beim Speichern der Gruppe", e);
            throw new RuntimeException("Fehler beim Erstellen der Gruppe", e);
        }

        return incomingGroup;
    }

    @Override
    public Optional<GroupResource> getGroup(String id) {
        return groupRepository.findById(id).map(dbGroup -> {
            try {
                return JsonUtils.getObjectReader()
                        .forType(GroupResource.class)
                        .readValue(dbGroup.getScimData());
            } catch (Exception e) {
                log.error("Fehler beim Parsen der Gruppe {}", id, e);
                throw new RuntimeException("Fehler beim Lesen der Gruppe", e);
            }
        });
    }

    @Transactional
    @Override
    public Optional<GroupResource> patchGroup(String groupId, PatchRequest patchRequest) {
        log.info("Verarbeite PATCH-Request für Gruppe ID: {}", groupId);

        return groupRepository.findById(groupId).map(dbGroup -> {
            try {
                GroupResource scimGroup = JsonUtils.getObjectReader()
                        .forType(GroupResource.class).readValue(dbGroup.getScimData());

                for (PatchOperation op : patchRequest.getOperations()) {
                    String path = op.getPath() != null ? op.getPath().toString() : "";

                    if (op.getOpType() == PatchOpType.ADD && path.equals("members")) {
                        op.getJsonNode().forEach(memberNode -> {
                            String userId = memberNode.get("value").asString();
                            String userDisplay = memberNode.has("display") ? memberNode.get("display").asString() : "";

                            if (scimGroup.getMembers() == null) {
                                scimGroup.setMembers(new ArrayList<>());
                            }

                            boolean alreadyMember = scimGroup.getMembers().stream()
                                    .anyMatch(m -> m.getValue().equals(userId));

                            if (!alreadyMember) {
                                Member newMember = new Member();
                                newMember.setValue(userId);
                                newMember.setDisplay(userDisplay);
                                scimGroup.getMembers().add(newMember);

                                updateUserWithNewGroup(userId, scimGroup, false);
                            }
                        });
                    }

                    if (op.getOpType() == PatchOpType.REMOVE && path.startsWith("members")) {
                        if (op.getPath().getElement(0).getValueFilter() != null) {
                            String userIdToRemove = op.getPath().getElement(0).getValueFilter().getComparisonValue().asString();

                            if (scimGroup.getMembers() != null) {
                                scimGroup.getMembers().removeIf(m -> m.getValue().equals(userIdToRemove));
                                updateUserWithNewGroup(userIdToRemove, scimGroup, true);
                            }
                        }
                    }
                }

                scimGroup.getMeta().setLastModified(Calendar.getInstance());
                dbGroup.setScimData(JsonUtils.getObjectWriter().writeValueAsString(scimGroup));
                groupRepository.save(dbGroup);

                return scimGroup;

            } catch (Exception e) {
                log.error("Fehler beim Patchen der Gruppe {}", groupId, e);
                throw new RuntimeException("Gruppen-Patch fehlgeschlagen", e);
            }
        });
    }

    @Override
    public void updateUserWithNewGroup(String userId, GroupResource scimGroup, boolean remove) {
        userRepository.findById(userId).ifPresent(dbUser -> {
            try {
                UserResource scimUser = JsonUtils.getObjectReader()
                        .forType(UserResource.class).readValue(dbUser.getScimData());

                if (scimUser.getGroups() == null) {
                    scimUser.setGroups(new ArrayList<>());
                }

                if (remove) {
                    scimUser.getGroups().removeIf(g -> g.getValue().equals(scimGroup.getId()));
                } else {
                    com.unboundid.scim2.common.types.Group groupRef = new com.unboundid.scim2.common.types.Group();
                    groupRef.setValue(scimGroup.getId());
                    groupRef.setDisplay(scimGroup.getDisplayName());
                    groupRef.setRef(scimGroup.getMeta().getLocation());
                    scimUser.getGroups().add(groupRef);
                }

                scimUser.getMeta().setLastModified(Calendar.getInstance());

                dbUser.setScimData(JsonUtils.getObjectWriter().writeValueAsString(scimUser));
                userRepository.save(dbUser);
                log.info("User {} erfolgreich mit Gruppen-Update synchronisiert", userId);

            } catch (Exception e) {
                log.error("Konnte User {} nicht synchronisieren", userId, e);
                throw new RuntimeException("Konnte User nicht mit Gruppe aktualisieren", e);
            }
        });
    }
}
