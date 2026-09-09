package spring.and.scim.de.prototype.controller;

import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.UserResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.and.scim.de.prototype.service.ScimGroupServiceImpl;
import spring.and.scim.de.prototype.advise.UserNotFoundException;

@Slf4j
@RestController
@RequestMapping(value = "/scim/v2/Groups", produces = "application/scim+json")
@Tag(name = "SCIM 2.0 Group Provisioning", description = "SCIM Group Prototype")
public class ScimGroupController {

    private final ScimGroupServiceImpl scimGroupService;

    public ScimGroupController(ScimGroupServiceImpl scimGroupService) {
        this.scimGroupService = scimGroupService;
    }

    @PostMapping(consumes = "application/scim+json")
    @Operation(summary = "Neuen Gruppe anlegen", description = "Speichert eine SCIM-Gruppe")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Benutzer erfolgreich angelegt",
                    content = @Content(schema = @Schema(implementation = GroupResource.class))),
            @ApiResponse(responseCode = "400", description = "Ungültiges Format")
    })
    public ResponseEntity<GroupResource> createGroup(@RequestBody GroupResource incomingGroup) {
        GroupResource createdGroup = scimGroupService.createGroup(incomingGroup);
        return ResponseEntity
                .created(createdGroup.getMeta().getLocation())
                .body(createdGroup);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gespeicherte Gruppen abrufen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gruppe gefunden"),
            @ApiResponse(responseCode = "404", description = "Gruppe nicht gefunden", content = @Content)
    })
    public ResponseEntity<GroupResource> getGroup(@PathVariable String id) {
        return scimGroupService.getGroup(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new UserNotFoundException("Die Gruppe mit der ID " + id + " existiert nicht."));
    }

    @PatchMapping(value = "/{id}", consumes = "application/scim+json")
    @Operation(summary = "Gruppe aktualisieren", description = "Führt partielle Updates aus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Update erfolgreich"),
            @ApiResponse(responseCode = "404", description = "Gruppe nicht gefunden"),
            @ApiResponse(responseCode = "400", description = "Fehlerhafte Patch-Syntax")
    })
    public ResponseEntity<GroupResource> patchUser(
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = Object.class))
            )
            @RequestBody PatchRequest patchRequest) {

        log.info("Eingehender PATCH-Request für User ID: {}", id);

        return scimGroupService.patchGroup(id, patchRequest)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Gruppe löschen", description = "Löscht eine SCIM-Gruppe anhand seiner ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Löschung erfolgreich (Kein Inhalt)"),
            @ApiResponse(responseCode = "404", description = "Gruppe nicht gefunden"),
            @ApiResponse(responseCode = "400", description = "Ungültige Anfrage (z. B. fehlerhafte ID-Syntax)")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {

        log.info("SCIM Delete User: {}", id);

        scimGroupService.deleteScimGroup(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping()
    @Operation(summary = "Gruppen suchen & filtern", description = "SCIM-Filter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gruppen Filtern erfolgreich"),
            @ApiResponse(responseCode = "404", description = "Gruppen mit Filter nicht gefunden"),
            @ApiResponse(responseCode = "400", description = "Fehlerhafte Filter-Syntax")
    })
    public ResponseEntity<ListResponse<GroupResource>> searchUsers(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false, defaultValue = "1") int startIndex,
            @RequestParam(required = false, defaultValue = "100") int count) {

        log.info("Suche Users. Filter: '{}', Start: {}, Count: {}", filter, startIndex, count);

        ListResponse<GroupResource> response = scimGroupService.searchScimGroups(filter, startIndex, count);
        return ResponseEntity.ok(response);
    }
}
