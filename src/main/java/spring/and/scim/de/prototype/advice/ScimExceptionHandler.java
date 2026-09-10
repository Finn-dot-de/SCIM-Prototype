package spring.and.scim.de.prototype.advice;

import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.messages.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import spring.and.scim.de.prototype.scim.ScimErrorException;
import spring.and.scim.de.prototype.scim.ScimMediaType;

/**
 * Rendert jeden Fehler als SCIM-ErrorResponse (RFC 7644, Abschnitt 3.12) —
 * IdPs erwarten dieses Format, nicht Springs Default-JSON.
 * <p>
 * Die Klasse erbt von {@link ResponseEntityExceptionHandler}, weil Spring die
 * Statuszuordnung fuer seine eigenen MVC-Fehler bereits mitbringt (405, 415,
 * 404 bei unbekannter Route, 400 bei falschem Parametertyp). Ersetzt wird nur
 * der Antwortkoerper; neue Spring-Fehlertypen sind damit automatisch abgedeckt.
 * <p>
 * Der Fallback gibt die Meldung der urspruenglichen Exception nicht an den
 * Client weiter: Interna wie SQL- oder Verbindungsfehler gehoeren ins Log, nicht
 * in eine HTTP-Antwort. Zusammengebaut wird die Antwort an genau einer Stelle,
 * in {@link #handleExceptionInternal}.
 */
@Slf4j
@RestControllerAdvice
public class ScimExceptionHandler extends ResponseEntityExceptionHandler {

    /** Fachliche Fehler: Status, scimType und Text stehen bereits fest. */
    @ExceptionHandler(ScimErrorException.class)
    public ResponseEntity<Object> handleScimError(ScimErrorException ex, WebRequest request) {
        return respond(ex.error(), ex, request);
    }

    /** Fehler des SCIM-SDK, die nicht schon im Service uebersetzt wurden. */
    @ExceptionHandler(ScimException.class)
    public ResponseEntity<Object> handleSdkError(ScimException ex, WebRequest request) {
        return respond(ex.getScimError(), ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unerwarteter Fehler bei der SCIM-Verarbeitung", ex);
        return respond(
                scimError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Interner Fehler im SCIM-Endpunkt."),
                ex, request);
    }

    /**
     * Gemeinsamer Ausgang aller Faelle: Spring hat den Status bestimmt, hier
     * wird der Koerper zur SCIM-ErrorResponse und der Content-Type gesetzt.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Object scimBody = (body instanceof ErrorResponse)
                ? body
                : scimError(status.value(), ex.getMessage());

        return super.handleExceptionInternal(ex, scimBody, scimHeaders(headers), status, request);
    }

    private ResponseEntity<Object> respond(ErrorResponse error, Exception ex, WebRequest request) {
        return handleExceptionInternal(
                ex, error, new HttpHeaders(), HttpStatusCode.valueOf(error.getStatus()), request);
    }

    private static ErrorResponse scimError(int status, @Nullable String detail) {
        ErrorResponse error = new ErrorResponse(status);
        error.setDetail(detail);
        return error;
    }

    private static HttpHeaders scimHeaders(HttpHeaders headers) {
        HttpHeaders scimHeaders = new HttpHeaders();
        scimHeaders.putAll(headers);
        scimHeaders.setContentType(ScimMediaType.SCIM_JSON);
        return scimHeaders;
    }
}
