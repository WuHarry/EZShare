package exceptions;

/**
 * Thrown when a server supplied by a client cannot be resolved
 */
public class InvalidServerException extends Exception {

    private static final long serialVersionUID = -641896925059277211L;

    public InvalidServerException(String message) {
        super(message);
    }

}
