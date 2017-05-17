package Exceptions;

import java.util.logging.Logger;

/**
 * Thrown when a command is sent by a client without all fields which are needed for this type
 * of command.
 */
public class MissingComponentException extends Exception {

    private static Logger logger = Logger.getLogger(MissingComponentException.class.getName());
    private static final long serialVersionUID = -1341167278866437350L;

    public MissingComponentException(String message) {
        super(message);
        logger.warning(message);
    }
}
