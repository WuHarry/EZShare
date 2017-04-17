package EZShare;

import JSON.JSONReader;
import exceptions.MissingComponentException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;

/**
 * Created by Yahang Wu on 2017/4/11.
 * COMP90015 Distributed System Project1 EZServer
 * It is the configuration class, mainly provide the method to load log config.
 */

class Common {
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
    public static boolean validateString(String s) {
        return s.length() == 0 || !(s.contains("\0") || s.charAt(0) == ' ' || s.charAt(s.length() - 1) == ' ');
    }

    /**
     * Returns true only if the described resource is made up of valid components (in terms of
     * String composition, not particular logic of a command).
     *
     * @param name
     * @param desc
     * @param tags
     * @param uri
     * @param channel
     * @param owner
     * @return
     */
    public static boolean validateResource(String name, String desc, String[] tags, String uri,
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
    public static void checkNull(JSONReader curr) throws MissingComponentException {
        if (curr.getResourceName() == null || curr.getResourceChannel() == null || curr.getResourceUri() == null ||
                curr.getResourceDescription() == null || curr.getResourceOwner() == null || curr.getResourceTags() == null) {
            throw new MissingComponentException("Missing resource.");
        }
        // to be continued
    }
}
