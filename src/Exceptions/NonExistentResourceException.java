package Exceptions;

/**
 * Thrown when an operation is attempted on a resource which does not exist.
 */
public class NonExistentResourceException extends Exception {

    private static final long serialVersionUID = 7390571345215152766L;

    public NonExistentResourceException(String message) {
        super(message);
    }
}
