package spring.and.scim.de.prototype.controller;

import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.UserResource;
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
import spring.and.scim.de.prototype.service.ScimUserService;

/**
 * SCIM-2.0-Endpunkt fuer Benutzer.
 * <p>
 * Reines HTTP-Mapping: jede Methode delegiert an den Service. Fehlerfaelle
 * wirft der Service, gerendert werden sie zentral im
 * {@code ScimExceptionHandler} — deshalb gibt es hier weder try/catch noch
 * {@code Optional}-Auswertung.
 */
@RestController
@RequestMapping(value = ScimPaths.USERS, produces = ScimMediaType.SCIM_JSON_VALUE)
@Tag(name = "SCIM 2.0 User Provisioning", description = "SCIM User Prototype")
@RequiredArgsConstructor
public class ScimUserController {

    private final ScimUserService userService;

    @PostMapping(consumes = ScimMediaType.SCIM_JSON_VALUE)
    @Operation(summary = "Neuen Benutzer anlegen",
            description = "Speichert einen SCIM-Benutzer über den Service.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Benutzer erfolgreich angelegt",
                    content = @Content(schema = @Schema(implementation = UserResource.class))),
            @ApiResponse(responseCode = "400", description = "Ungültiges Format"),
            @ApiResponse(responseCode = "409", description = "userName bereits vergeben")
    })
    public ResponseEntity<UserResource> createUser(@RequestBody UserResource user) {
        UserResource created = userService.create(user);
        return ResponseEntity.created(created.getMeta().getLocation()).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gespeicherten Benutzer abrufen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Benutzer gefunden"),
            @ApiResponse(responseCode = "404", description = "Benutzer nicht gefunden",
                    content = @Content)
    })
    public UserResource getUser(@PathVariable String id) {
        return userService.findById(id);
    }

    @GetMapping
    @Operation(summary = "Benutzer suchen & filtern", description = "SCIM-Filter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suche erfolgreich"),
            @ApiResponse(responseCode = "400", description = "Fehlerhafte Filter-Syntax")
    })
    public ListResponse<UserResource> searchUsers(
            @RequestParam(required = false) @Nullable String filter,
            @RequestParam(defaultValue = "1") int startIndex,
            @RequestParam(defaultValue = "100") int count) {

        return userService.search(filter, startIndex, count);
    }

    @PatchMapping(value = "/{id}", consumes = ScimMediaType.SCIM_JSON_VALUE)
    @Operation(summary = "Benutzer aktualisieren",
            description = "Führt partielle Updates aus, z.B. nur Name oder Status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Update erfolgreich"),
            @ApiResponse(responseCode = "400", description = "Fehlerhafte Patch-Syntax"),
            @ApiResponse(responseCode = "404", description = "Benutzer nicht gefunden")
    })
    public UserResource patchUser(@PathVariable String id,
                                  @RequestBody PatchRequest patchRequest) {
        return userService.patch(id, patchRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Benutzer löschen",
            description = "Löscht einen SCIM-Benutzer anhand seiner ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Löschung erfolgreich"),
            @ApiResponse(responseCode = "400", description = "Ungültige ID-Syntax"),
            @ApiResponse(responseCode = "404", description = "Benutzer nicht gefunden")
    })
    public void deleteUser(@PathVariable String id) {
        userService.delete(id);
    }
}
