package exceptions;

/**
 * Indicates that the secret a client uses in a command to the server does not match the
 * server's secret.
 */
public class IncorrectSecretException extends Exception {

    private static final long serialVersionUID = -7883903467530862433L;

    public IncorrectSecretException(String message) {
        super(message);
    }
}
