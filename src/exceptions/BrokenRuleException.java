package exceptions;

import java.util.logging.Logger;

/**
 * Thrown when attempt to publish a resource supplied by a client
 * contains same channel and uri but different owner
 */
public class BrokenRuleException extends Exception {

    private static Logger logger = Logger.getLogger(BrokenRuleException.class.getName());
    private static final long serialVersionUID = 7499045713237494907L;

    public BrokenRuleException(String message) {
        super(message);
        logger.warning(message);
    }
}
