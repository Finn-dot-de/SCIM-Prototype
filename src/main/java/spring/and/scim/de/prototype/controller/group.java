package spring.and.scim.de.prototype.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "scim_groups")
@Setter
@Getter
public class GroupEntity {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String displayName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String scimData;

    public GroupEntity(String id, String displayName, String scimData) {
        this.id = id;
        this.displayName = displayName;
        this.scimData = scimData;
    }

    public GroupEntity() {}
}

package spring.and.scim.de.prototype.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.and.scim.de.prototype.entity.GroupEntity;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, String> {
}


package spring.and.scim.de.prototype.Service;

import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.Meta;
import com.unboundid.scim2.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.and.scim.de.prototype.entity.GroupEntity;
import spring.and.scim.de.prototype.repository.GroupRepository;

import java.net.URI;
import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ScimGroupServiceImpl {

    private final GroupRepository groupRepository;

    public ScimGroupServiceImpl(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public GroupResource createGroup(GroupResource incomingGroup) {
        log.info("Verarbeite SCIM Group-Erstellung für: {}", incomingGroup.getDisplayName());

        String newId = UUID.randomUUID().toString();
        incomingGroup.setId(newId);

        Meta meta = new Meta();
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
            throw new RuntimeException("Fehler beim Speichern der Gruppe", e);
        }

        return incomingGroup;
    }

    public Optional<GroupResource> getGroup(String id) {
        return groupRepository.findById(id).map(dbGroup -> {
            try {
                return JsonUtils.getObjectReader()
                        .forType(GroupResource.class)
                        .readValue(dbGroup.getScimData());
            } catch (Exception e) {
                throw new RuntimeException("Fehler beim Lesen der Gruppe", e);
            }
        });
    }
}



package spring.and.scim.de.prototype.controller;

import com.unboundid.scim2.common.types.GroupResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.and.scim.de.prototype.Service.ScimGroupServiceImpl;
import spring.and.scim.de.prototype.advise.UserNotFoundException;

@Slf4j
@RestController
@RequestMapping(value = "/scim/v2/Groups", produces = "application/scim+json")
public class ScimGroupController {

    private final ScimGroupServiceImpl scimGroupService;

    public ScimGroupController(ScimGroupServiceImpl scimGroupService) {
        this.scimGroupService = scimGroupService;
    }

    @PostMapping(consumes = "application/scim+json")
    public ResponseEntity<GroupResource> createGroup(@RequestBody GroupResource incomingGroup) {
        GroupResource createdGroup = scimGroupService.createGroup(incomingGroup);
        return ResponseEntity
                .created(createdGroup.getMeta().getLocation())
                .body(createdGroup);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResource> getGroup(@PathVariable String id) {
        return scimGroupService.getGroup(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new UserNotFoundException("Die Gruppe mit der ID " + id + " existiert nicht."));
    }
}
