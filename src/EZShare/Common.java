package EZShare;

import JSON.JSONReader;
import exceptions.InvalidResourceException;
import exceptions.MissingComponentException;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.LogManager;

/**
 * Created by Yahang Wu on 2017/4/11.
 * COMP90015 Distributed System Project1 EZServer
 * It is the configuration class, mainly provide the method to load log config.
 */

class Common {

    /**
     * The method to load log properties file
     */
    static void logConfig() {
        //Load log config file
        try {
            FileInputStream config = new FileInputStream("logger.properties");
            LogManager.getLogManager().readConfiguration(config);

        } catch (IOException e) {
            System.out.println("WARNING: Can not load log configuration file");
            System.out.println("WARNING: Logging not configured");
            e.printStackTrace();
        }
    }

    /**
     * Returns true only if the String s is valid according to rules for resource field
     * strings supplied to the server.
     *
     * @param s String to be checked.
     * @return True if s is valid, false otherwise.
     */
    private static boolean validateString(String s) {
        return s.length() == 0 || !(s.contains("\0") || s.charAt(0) == ' ' || s.charAt(s.length() - 1) == ' ');
    }

    /**
     * Returns true only if the described resource is made up of valid components (in terms of
     * String composition, not particular logic of a command).
     *
     * @param name    the resource's name
     * @param desc    the resource's description
     * @param tags    the resource's the tags
     * @param uri     the resource's uri
     * @param channel the resource's channel
     * @param owner   the resource's owner
     * @return return true if the resource is valid,
     * false if the resource is invalid
     */
    static boolean validateResource(String name, String desc, String[] tags, String uri,
                                    String channel, String owner) {
        if (!(validateString(name) && validateString(desc) && validateString(channel) &&
                validateString(owner) && validateString(uri))) {
            //Error with resource
            return false;
        }
        for (String tag : tags) {
            if (!validateString(tag)) {
                return false;
            }
        }
        return !owner.equals("*");
    }

    /**
     * Throws exception if curr does not contain fields necessary to describe a resource.
     *
     * @param curr The JSONReader which will be checked for a complete resource.
     *             output Output to write to.
     * @throws MissingComponentException Thrown if curr does not contain full resource descriptor.
     */
    static void checkNull(JSONReader curr) throws MissingComponentException {
        if (curr.getResourceName() == null || curr.getResourceChannel() == null || curr.getResourceUri() == null ||
                curr.getResourceDescription() == null || curr.getResourceOwner() == null || curr.getResourceTags() == null) {
            throw new MissingComponentException("Missing resource.");
        }
    }

    /**
     * The method to check whether the uri is valid if not throw Exceptions
     *
     * @param uri     the uri to test
     * @param command the command the server is processing
     * @throws InvalidResourceException If the resource supplied contains illegal fields, this is thrown.
     */
    static void validUri(String uri, String command) throws InvalidResourceException {
        try {
            URI path = new URI(uri);
            if (!path.isAbsolute() || path.getScheme().equals("file")) {
                throw new InvalidResourceException("Trying to " + command + " resource with non-absolute or file uri.");
            }
        } catch (URISyntaxException e) {
            throw new InvalidResourceException("Attempting to " + command + " resource with invalid uri syntax.");
        }
    }
}
