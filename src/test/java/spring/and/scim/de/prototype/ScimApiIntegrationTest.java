package spring.and.scim.de.prototype;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import spring.and.scim.de.prototype.repository.UserRepository;
import spring.and.scim.de.prototype.scim.ScimMediaType;
import tools.jackson.databind.json.JsonMapper;

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
 * Prüft die SCIM-Endpunkte über alle Schichten gegen die echte Datenbank.
 * <p>
 * {@code @Transactional} sorgt dafür, dass jeder Test am Ende zurückgerollt
 * wird — die Entwicklungsdatenbank bleibt unverändert.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ScimApiIntegrationTest {

    private static final String ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    private String createUser(String userName) throws Exception {
        MvcResult result = mvc.perform(post("/scim/v2/Users")
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content("""
                                {
                                  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                                  "userName": "%s",
                                  "name": { "givenName": "Max", "familyName": "Mustermann" },
                                  "emails": [ { "value": "max@example.com", "primary": true } ],
                                  "active": true
                                }""".formatted(userName)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.meta.resourceType").value("User"))
                .andExpect(jsonPath("$.meta.created").isNotEmpty())
                .andReturn();

        return JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString())
                .get("id").asString();
    }

    @Test
    void createReadDeleteRoundTrip() throws Exception {
        String id = createUser("roundtrip");

        mvc.perform(get("/scim/v2/Users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("roundtrip"))
                .andExpect(jsonPath("$.active").value(true));

        mvc.perform(delete("/scim/v2/Users/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/scim/v2/Users/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA));
    }

    @Test
    void patchDeactivatesUserAndKeepsCreatedTimestamp() throws Exception {
        String id = createUser("offboarding");

        mvc.perform(patch("/scim/v2/Users/" + id)
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content("""
                                {
                                  "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                                  "Operations": [
                                    { "op": "replace", "path": "active", "value": false }
                                  ]
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.userName").value("offboarding"))
                .andExpect(jsonPath("$.meta.created").isNotEmpty());
    }

    /**
     * Ein PATCH auf {@code userName} muss auch die Spalte nachziehen, auf der
     * die Unique-Constraint und der Duplikat-Check arbeiten.
     */
    @Test
    void patchOfUserNameAlsoUpdatesTheIndexedColumn() throws Exception {
        String id = createUser("alter.name");

        mvc.perform(patch("/scim/v2/Users/" + id)
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content("""
                                {
                                  "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                                  "Operations": [
                                    { "op": "replace", "path": "userName", "value": "neuer.name" }
                                  ]
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("neuer.name"));

        assertThat(userRepository.findByBusinessKey("neuer.name")).isPresent();
        assertThat(userRepository.findByBusinessKey("alter.name")).isEmpty();
    }

    @Test
    void duplicateUserNameYieldsConflictNotServerError() throws Exception {
        createUser("duplikat");

        mvc.perform(post("/scim/v2/Users")
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content("""
                                {
                                  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                                  "userName": "duplikat"
                                }"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.scimType").value("uniqueness"))
                .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA));
    }

    @Test
    void missingUserNameIsRejected() throws Exception {
        mvc.perform(post("/scim/v2/Users")
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content("""
                                {"schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.scimType").value("invalidValue"));
    }

    @Test
    void filterSupportsMoreThanEquality() throws Exception {
        createUser("filter.eins");
        createUser("filter.zwei");

        mvc.perform(get("/scim/v2/Users").param("filter", "userName eq \"filter.eins\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.Resources[0].userName").value("filter.eins"));

        mvc.perform(get("/scim/v2/Users").param("filter", "userName sw \"filter.\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(2));

        mvc.perform(get("/scim/v2/Users")
                        .param("filter", "userName sw \"filter.\" and active eq true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(2));
    }

    @Test
    void brokenFilterIsBadRequestNotAnEmptyResult() throws Exception {
        mvc.perform(get("/scim/v2/Users").param("filter", "userName ==== \"x\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA));
    }

    @Test
    void itemsPerPageReportsWhatWasActuallyReturned() throws Exception {
        createUser("seite.a");
        createUser("seite.b");

        mvc.perform(get("/scim/v2/Users")
                        .param("filter", "userName sw \"seite.\"")
                        .param("startIndex", "2")
                        .param("count", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(2))
                .andExpect(jsonPath("$.startIndex").value(2))
                .andExpect(jsonPath("$.itemsPerPage").value(1));

        mvc.perform(get("/scim/v2/Users").param("startIndex", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsPerPage").value(0));
    }

    @Test
    void malformedIdIsRejectedConsistentlyAcrossVerbs() throws Exception {
        mvc.perform(get("/scim/v2/Users/keine-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.scimType").value("invalidValue"));

        mvc.perform(delete("/scim/v2/Users/keine-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void groupsBehaveLikeUsers() throws Exception {
        mvc.perform(post("/scim/v2/Groups")
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content("""
                                {
                                  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
                                  "displayName": "Sachbearbeitung"
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.meta.resourceType").value("Group"))
                .andExpect(jsonPath("$.displayName").value("Sachbearbeitung"));

        mvc.perform(post("/scim/v2/Groups")
                        .contentType(ScimMediaType.SCIM_JSON)
                        .content("""
                                {
                                  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
                                  "displayName": "Sachbearbeitung"
                                }"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.scimType").value("uniqueness"));
    }

    /**
     * Diese Fälle behandelt kein eigener Handler — sie kommen daher, dass der
     * ScimExceptionHandler von ResponseEntityExceptionHandler erbt.
     */
    @Test
    void springMvcErrorsAlsoArriveAsScimErrorResponses() throws Exception {
        mvc.perform(get("/scim/v2/Users").param("startIndex", "keine-zahl"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA));

        mvc.perform(put("/scim/v2/Users"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.schemas[0]").value(ERROR_SCHEMA));
    }
}
