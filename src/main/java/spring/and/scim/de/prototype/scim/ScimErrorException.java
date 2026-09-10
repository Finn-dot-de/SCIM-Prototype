package spring.and.scim.de.prototype.scim;

import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.messages.ErrorResponse;
import org.jspecify.annotations.Nullable;

/**
 * Einzige fachliche Fehlerklasse dieser Anwendung. Sie traegt die fertige
 * SCIM-{@link ErrorResponse} (RFC 7644, Abschnitt 3.12) mit sich, also Status,
 * {@code scimType} und Detailtext.
 * <p>
 * Ein Typ statt mehrerer, weil niemand die Faelle unterschiedlich <em>faengt</em> —
 * sie werden ausschliesslich vom {@code ScimExceptionHandler} in eine Antwort
 * gerendert. Die statischen Fabrikmethoden machen den Aufruf am Werfen-Ort
 * lesbar und halten die Fehlertexte an einer Stelle.
 */
public class ScimErrorException extends RuntimeException {

    private static final int BAD_REQUEST = 400;
    private static final int NOT_FOUND = 404;
    private static final int CONFLICT = 409;

    /** {@code ErrorResponse} ist nicht serialisierbar, die Exception aber schon. */
    private final transient ErrorResponse error;

    private ScimErrorException(ErrorResponse error, @Nullable Throwable cause) {
        super(error.getDetail(), cause);
        this.error = error;
    }

    public static ScimErrorException notFound(ScimResourceType type, String id) {
        return new ScimErrorException(
                errorResponse(NOT_FOUND, null,
                        "%s mit der ID '%s' existiert nicht.".formatted(type.typeName(), id)),
                null);
    }

    public static ScimErrorException conflict(ScimResourceType type, String uniqueValue) {
        return new ScimErrorException(
                errorResponse(CONFLICT, "uniqueness",
                        "%s mit %s '%s' existiert bereits.".formatted(
                                type.typeName(), type.uniqueAttribute(), uniqueValue)),
                null);
    }

    public static ScimErrorException badRequest(String scimType, String detail) {
        return new ScimErrorException(errorResponse(BAD_REQUEST, scimType, detail), null);
    }

    /**
     * Uebernimmt einen Fehler des SCIM-SDK (fehlerhafter Filter, ungueltiger
     * Patch-Pfad, ...) samt dem Status und {@code scimType}, den das SDK dafuer
     * bereits korrekt gesetzt hat.
     */
    public static ScimErrorException from(ScimException cause) {
        return new ScimErrorException(cause.getScimError(), cause);
    }

    public ErrorResponse error() {
        return error;
    }

    private static ErrorResponse errorResponse(int status, @Nullable String scimType, String detail) {
        ErrorResponse response = new ErrorResponse(status);
        response.setDetail(detail);
        if (scimType != null) {
            response.setScimType(scimType);
        }
        return response;
    }
}
