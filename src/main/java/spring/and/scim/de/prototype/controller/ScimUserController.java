package spring.and.scim.de.prototype.controller;

import com.unboundid.scim2.common.exceptions.BadRequestException;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.messages.PatchRequest;
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
import spring.and.scim.de.prototype.Service.ScimUserService;
import spring.and.scim.de.prototype.advise.UserNotFoundException;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(value = "/scim/v2/Users", produces = "application/scim+json")
@Tag(name = "SCIM 2.0 User Provisioning", description = "SCIM User Prototype")
public class ScimUserController {

    private final ScimUserService scimUserService;

    public ScimUserController(ScimUserService scimUserService) {
        this.scimUserService = scimUserService;
    }

    @PostMapping(consumes = "application/scim+json")
    @Operation(summary = "Neuen Benutzer anlegen", description = "Speichert einen SCIM-Benutzer über den Service.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Benutzer erfolgreich angelegt",
                    content = @Content(schema = @Schema(implementation = UserResource.class))),
            @ApiResponse(responseCode = "400", description = "Ungültiges Format")
    })
    public ResponseEntity<UserResource> createUser(@RequestBody UserResource incomingUser) {

        UserResource createdUser = scimUserService.createUser(incomingUser);

        return ResponseEntity
                .created(createdUser.getMeta().getLocation())
                .body(createdUser);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gespeicherte Benutzer abrufen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Benutzer gefunden"),
            @ApiResponse(responseCode = "404", description = "Benutzer nicht gefunden", content = @Content)
    })
    public ResponseEntity<UserResource> getUser(@PathVariable String id) {
        return scimUserService.getUser(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new UserNotFoundException("Der User mit der ID " + id + " existiert nicht."));
    }

    @PatchMapping(value = "/{id}", consumes = "application/scim+json")
    @Operation(summary = "Benutzer aktualisieren", description = "Führt partielle Updates (z.B. nur Name oder Status) aus.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Update erfolgreich"),
            @ApiResponse(responseCode = "404", description = "User nicht gefunden"),
            @ApiResponse(responseCode = "400", description = "Fehlerhafte Patch-Syntax")
    })
    public ResponseEntity<UserResource> patchUser(
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = Object.class))
            )
            @RequestBody PatchRequest patchRequest) {

        log.info("Eingehender PATCH-Request für User ID: {}", id);

        return scimUserService.patchUser(id, patchRequest)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Benutzer suchen & filtern", description = "SCIM-Filter")
    public ResponseEntity<ListResponse<UserResource>> searchUsers(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false, defaultValue = "1") int startIndex,
            @RequestParam(required = false, defaultValue = "100") int count) {

        log.info("Suche Users. Filter: '{}', Start: {}, Count: {}", filter, startIndex, count);

        ListResponse<UserResource> response = scimUserService.searchUsers(filter, startIndex, count);
        return ResponseEntity.ok(response);
    }

}
