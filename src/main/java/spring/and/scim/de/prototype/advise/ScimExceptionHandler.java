package spring.and.scim.de.prototype.advise;

import com.unboundid.scim2.common.messages.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ScimExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException e) {

        ErrorResponse scimError = new ErrorResponse(404);
        scimError.setDetail(e.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("Content-Type", "application/scim+json")
                .body(scimError);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            com.fasterxml.jackson.core.JsonProcessingException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {

        ErrorResponse scimError = new ErrorResponse(400);
        scimError.setDetail("Syntax-Fehler im Request: " + e.getMessage());
        scimError.setScimType("invalidSyntax");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/scim+json")
                .body(scimError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalError(Exception e) {

        ErrorResponse scimError = new ErrorResponse(500);
        scimError.setDetail("Interner Serverfehler im SCIM: " + e.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "application/scim+json")
                .body(scimError);
    }
}
