package spring.and.scim.de.prototype.scim;

/**
 * Die von RFC 7644 vorgegebenen Endpunkt-Pfade an einer Stelle.
 * <p>
 * Konstanten (keine Enum-Methoden), weil sie in {@code @RequestMapping}
 * verwendet werden und Annotationen Compile-Time-Konstanten verlangen.
 */
public final class ScimPaths {

    public static final String BASE = "/scim/v2";

    public static final String USERS = BASE + "/Users";

    public static final String GROUPS = BASE + "/Groups";

    private ScimPaths() {
    }
}
