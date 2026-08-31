package scim.bamf.in.bund.de.spring.and.scim.springandscim.controller;

import com.unboundid.scim2.common.types.UserResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import scim.bamf.in.bund.de.spring.and.scim.springandscim.Service.ScimUserService;

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
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
