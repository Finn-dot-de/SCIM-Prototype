package spring.and.scim.de.prototype.scim;

/**
 * Beschreibt eine SCIM-Ressourcenart an genau einer Stelle: wie sie in
 * {@code meta.resourceType} heisst, unter welchem Pfad sie liegt und welches
 * Attribut sie fachlich eindeutig macht.
 */
public enum ScimResourceType {

    USER("User", ScimPaths.USERS, "userName"),
    GROUP("Group", ScimPaths.GROUPS, "displayName");

    /** Wert fuer {@code meta.resourceType}. */
    private final String typeName;

    /** Endpunkt-Pfad, z.B. {@code /scim/v2/Users}. */
    private final String path;

    /** Fachlicher Schluessel: {@code userName} bzw. {@code displayName}. */
    private final String uniqueAttribute;

    ScimResourceType(String typeName, String path, String uniqueAttribute) {
        this.typeName = typeName;
        this.path = path;
        this.uniqueAttribute = uniqueAttribute;
    }

    public String typeName() {
        return typeName;
    }

    public String path() {
        return path;
    }

    public String uniqueAttribute() {
        return uniqueAttribute;
    }
}
