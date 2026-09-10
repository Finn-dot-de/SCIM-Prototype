package spring.and.scim.de.prototype;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import spring.and.scim.de.prototype.scim.ScimMediaType;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Faehrt den kompletten Demo-Ablauf aus TestJSON/scim-demo.http mit genau den
 * Dateien durch, die dabei verschickt werden. Schlaegt hier etwas fehl, wuerde
 * es auch in der Vorstellung fehlschlagen.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemoBeispieleTest {

    private static final Path DEMO = Path.of("TestJSON");

    @Autowired
    private MockMvc mvc;

    private String body(String datei) throws Exception {
        return Files.readString(DEMO.resolve(datei));
    }

    private String idOf(MvcResult result) throws Exception {
        return JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString())
                .get("id").asString();
    }

    @Test
    void demoAblaufLaeuftVollstaendigDurch() throws Exception {
        // --- Akt 1: Onboarding ------------------------------------------------
        mvc.perform(get("/scim/v2/Users").param("filter", "userName eq \"m.schneider@example.de\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(0));

        MvcResult angelegt = mvc.perform(post("/scim/v2/Users")
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content(body("01-user-anlegen.json")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.userName").value("m.schneider@example.de"))
                .andExpect(jsonPath("$.title").value("Sachbearbeiterin"))
                .andExpect(jsonPath("$.meta.resourceType").value("User"))
                // Die Enterprise-Extension muss den Weg durch die Datenbank ueberleben.
                .andExpect(jsonPath(
                        "$['urn:ietf:params:scim:schemas:extension:enterprise:2.0:User'].department")
                        .value("Referat 42"))
                // Umlaute muessen JSONB und Rueckweg unbeschadet ueberstehen.
                .andExpect(jsonPath("$.addresses[0].streetAddress").value("Musterstraße 1"))
                .andExpect(jsonPath(
                        "$['urn:ietf:params:scim:schemas:extension:enterprise:2.0:User'].organization")
                        .value("Beispielbehörde"))
                .andReturn();
        String userId = idOf(angelegt);

        MvcResult zweiter = mvc.perform(post("/scim/v2/Users")
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content(body("02-user-anlegen-zweiter.json")))
                .andExpect(status().isCreated())
                .andReturn();
        String userId2 = idOf(zweiter);

        mvc.perform(post("/scim/v2/Users")
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content(body("01-user-anlegen.json")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.scimType").value("uniqueness"));

        // --- Akt 2: Suchen und Filtern ---------------------------------------
        mvc.perform(get("/scim/v2/Users").param("filter", "userName eq \"m.schneider@example.de\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(1));

        mvc.perform(get("/scim/v2/Users")
                        .param("filter", "title co \"Sachbearbeit\" and active eq true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(2));

        mvc.perform(get("/scim/v2/Users").param("startIndex", "1").param("count", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsPerPage").value(1));

        // --- Akt 3: PATCH -----------------------------------------------------
        MvcResult vorher = mvc.perform(get("/scim/v2/Users/" + userId))
                .andExpect(status().isOk())
                .andReturn();
        String createdVorher = JsonMapper.builder().build()
                .readTree(vorher.getResponse().getContentAsString())
                .get("meta").get("created").asString();

        mvc.perform(patch("/scim/v2/Users/" + userId)
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content(body("03-user-patch-befoerderung.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Referatsleiterin"))
                .andExpect(jsonPath("$.name.givenName").value("Maria-Theresa"))
                .andExpect(jsonPath("$.userName").value("m.schneider@example.de"))
                .andExpect(jsonPath("$.emails.length()").value(2))
                .andExpect(jsonPath(
                        "$['urn:ietf:params:scim:schemas:extension:enterprise:2.0:User'].department")
                        .value("Referat 43"));

        MvcResult nachher = mvc.perform(get("/scim/v2/Users/" + userId))
                .andExpect(status().isOk())
                .andReturn();
        String createdNachher = JsonMapper.builder().build()
                .readTree(nachher.getResponse().getContentAsString())
                .get("meta").get("created").asString();
        assertThat(createdNachher)
                .as("meta.created darf ein PATCH nicht ueberschreiben")
                .isEqualTo(createdVorher);

        // --- Akt 4: Gruppen ---------------------------------------------------
        MvcResult gruppe = mvc.perform(post("/scim/v2/Groups")
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content(body("05-gruppe-anlegen.json")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meta.resourceType").value("Group"))
                .andExpect(jsonPath("$.displayName").value("Sachbearbeitung Asyl – Prüfgruppe"))
                .andReturn();
        String groupId = idOf(gruppe);

        String mitglieder = body("06-gruppe-patch-mitglied-hinzufuegen.json")
                .replace("HIER-USER-ID-EINSETZEN", userId);
        mvc.perform(patch("/scim/v2/Groups/" + groupId)
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content(mitglieder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[0].value").value(userId));

        mvc.perform(post("/scim/v2/Groups")
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content(body("05-gruppe-anlegen.json")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.scimType").value("uniqueness"));

        // --- Akt 5: Offboarding ----------------------------------------------
        mvc.perform(patch("/scim/v2/Users/" + userId2)
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content(body("04-user-patch-deaktivieren.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(delete("/scim/v2/Users/" + userId2))
                .andExpect(status().isNoContent());

        mvc.perform(get("/scim/v2/Users/" + userId2))
                .andExpect(status().isNotFound());

        // --- Akt 6: Fehlerformat ---------------------------------------------
        mvc.perform(post("/scim/v2/Users")
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content(body("90-fehler-user-ohne-username.json")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.scimType").value("invalidValue"));

        mvc.perform(get("/scim/v2/Users").param("filter", "userName ==== \"x\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.scimType").value("invalidFilter"));

        mvc.perform(get("/scim/v2/Users/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/scim/v2/Users/keine-gueltige-id"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/scim/v2/Users").contentType(ScimMediaType.SCIM_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        // --- Akt 7: Aufraeumen ------------------------------------------------
        mvc.perform(delete("/scim/v2/Users/" + userId)).andExpect(status().isNoContent());
        mvc.perform(delete("/scim/v2/Groups/" + groupId)).andExpect(status().isNoContent());
    }
}
