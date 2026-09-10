package spring.and.scim.de.prototype.scim;

import org.springframework.http.MediaType;

/**
 * Offizieller SCIM Media Type (RFC 7644). IdPs senden und erwarten diesen.
 */
public final class ScimMediaType {

    /** Fuer Annotationen, die eine Compile-Time-Konstante brauchen. */
    public static final String SCIM_JSON_VALUE = "application/scim+json";

    public static final MediaType SCIM_JSON = MediaType.parseMediaType(SCIM_JSON_VALUE);

    private ScimMediaType() {
    }
}
