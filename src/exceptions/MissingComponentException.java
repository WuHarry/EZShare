package exceptions;

/**
 * Thrown when a command is sent by a client without all fields which are needed for this type
 * of command.
 */
public class MissingComponentException extends Exception {

    private static final long serialVersionUID = -1341167278866437350L;

    public MissingComponentException() {
        super();
    }

    public MissingComponentException(String message) {
        super(message);
    }

}
