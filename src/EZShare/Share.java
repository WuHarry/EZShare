package EZShare;

import Connection.Connection;
import JSON.JSONReader;
import Resource.HashDatabase;
import Resource.Resource;
import exceptions.IncorrectSecretException;
import exceptions.InvalidResourceException;
import exceptions.MissingComponentException;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * Created by Yahang Wu on 2017/4/18.
 * COMP90015 Distributed System Project1 EZServer
 * Server share function to share resources
 */
class Share {

    /**
     * Validates then shares a resource with a file uri, inserting it into the database.
     *
     * @param resource The resource to be shared.
     * @param db       Database to insert the resource into.
     * @throws InvalidResourceException  If the resource contains incorrect information or invalid fields, this is thrown.
     * @throws IncorrectSecretException  If the secret supplied does not match server secret, this is thrown.
     * @throws MissingComponentException If secret is missing from command, this is thrown.
     */
    static void share(JSONReader resource, HashDatabase db, String serverSecret) throws InvalidResourceException, IncorrectSecretException, MissingComponentException {
        String name = resource.getResourceName();
        String description = resource.getResourceDescription();
        String channel = resource.getResourceChannel();
        String owner = resource.getResourceOwner();
        String uri = resource.getResourceUri();
        String[] tags = resource.getResourceTags();
        String ezserver = Connection.hostName + ":" + Server.port;
        String secret = resource.getSecret();

        if (secret == null) {
            //missing secret
            throw new MissingComponentException();
        }

        //Check secret
        if (!secret.equals(serverSecret)) {
            //Incorrect secret, error.
            throw new IncorrectSecretException();
        }

        //Validate strings
        if (!Common.validateResource(name, description, tags, uri, channel, owner)) {
            throw new InvalidResourceException("Trying to share Resource with illegal fields.");
        }
        //Validate uri
        try {
            URI path = new URI(uri);
            if (!path.isAbsolute() || !path.getScheme().equals("file")) {
                throw new InvalidResourceException("Trying to share resource with non-absolute or non-file uri.");
            }
            File f = new File(path);
            if (!f.exists() || f.isDirectory()) {
                throw new InvalidResourceException("File referenced by uri does not exist.");
            }
        } catch (URISyntaxException e) {
            throw new InvalidResourceException("Attempting to share resource with invalid uri syntax.");
        }

        //Remove if match pKey in db
        Resource match = db.pKeyLookup(channel, uri);
        if (match != null) {
            db.deleteResource(match);
        }

        //Add to db
        db.insertResource(new Resource(name, description, Arrays.asList(tags),
                uri, channel, owner, ezserver));
    }
}
