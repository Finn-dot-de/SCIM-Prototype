package spring.and.scim.de.prototype.controller;

import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.GroupResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import spring.and.scim.de.prototype.scim.ScimMediaType;
import spring.and.scim.de.prototype.scim.ScimPaths;
import spring.and.scim.de.prototype.service.ScimGroupService;

/**
 * SCIM-2.0-Endpunkt fuer Gruppen. Aufbau wie
 * {@link ScimUserController} — die Ablauflogik liegt im Service.
 */
@RestController
@RequestMapping(value = ScimPaths.GROUPS, produces = ScimMediaType.SCIM_JSON_VALUE)
@Tag(name = "SCIM 2.0 Group Provisioning", description = "SCIM Group Prototype")
@RequiredArgsConstructor
public class ScimGroupController {

    private final ScimGroupService groupService;

    @PostMapping(consumes = ScimMediaType.SCIM_JSON_VALUE)
    @Operation(summary = "Neue Gruppe anlegen", description = "Speichert eine SCIM-Gruppe.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Gruppe erfolgreich angelegt",
                    content = @Content(schema = @Schema(implementation = GroupResource.class))),
            @ApiResponse(responseCode = "400", description = "Ungültiges Format"),
            @ApiResponse(responseCode = "409", description = "displayName bereits vergeben")
    })
    public ResponseEntity<GroupResource> createGroup(@RequestBody GroupResource group) {
        GroupResource created = groupService.create(group);
        return ResponseEntity.created(created.getMeta().getLocation()).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gespeicherte Gruppe abrufen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gruppe gefunden"),
            @ApiResponse(responseCode = "404", description = "Gruppe nicht gefunden",
                    content = @Content)
    })
    public GroupResource getGroup(@PathVariable String id) {
        return groupService.findById(id);
    }

    @GetMapping
    @Operation(summary = "Gruppen suchen & filtern", description = "SCIM-Filter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suche erfolgreich"),
            @ApiResponse(responseCode = "400", description = "Fehlerhafte Filter-Syntax")
    })
    public ListResponse<GroupResource> searchGroups(
            @RequestParam(required = false) @Nullable String filter,
            @RequestParam(defaultValue = "1") int startIndex,
            @RequestParam(defaultValue = "100") int count) {

        return groupService.search(filter, startIndex, count);
    }

    @PatchMapping(value = "/{id}", consumes = ScimMediaType.SCIM_JSON_VALUE)
    @Operation(summary = "Gruppe aktualisieren", description = "Führt partielle Updates aus.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Update erfolgreich"),
            @ApiResponse(responseCode = "400", description = "Fehlerhafte Patch-Syntax"),
            @ApiResponse(responseCode = "404", description = "Gruppe nicht gefunden")
    })
    public GroupResource patchGroup(@PathVariable String id,
                                    @RequestBody PatchRequest patchRequest) {
        return groupService.patch(id, patchRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Gruppe löschen", description = "Löscht eine SCIM-Gruppe anhand ihrer ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Löschung erfolgreich"),
            @ApiResponse(responseCode = "400", description = "Ungültige ID-Syntax"),
            @ApiResponse(responseCode = "404", description = "Gruppe nicht gefunden")
    })
    public void deleteGroup(@PathVariable String id) {
        groupService.delete(id);
    }
}
