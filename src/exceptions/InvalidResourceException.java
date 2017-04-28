package exceptions;

/**
 * Thrown when a resource supplied by a client contains incorrect information which cannot
 * be recovered from.
 */
public class InvalidResourceException extends Exception {

    private static final long serialVersionUID = 3749904713237494907L;

    public InvalidResourceException(String message) {
        super(message);
    }

}
