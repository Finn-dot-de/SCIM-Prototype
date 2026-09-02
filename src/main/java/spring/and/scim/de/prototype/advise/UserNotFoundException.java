package spring.and.scim.de.prototype.advise;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) { super(message); }
}