package EZShare;

import JSON.JSONReader;
import Resource.HashDatabase;
import Resource.Resource;
import Exceptions.InvalidResourceException;
import Exceptions.MissingComponentException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Yahang Wu on 2017/4/24.
 * COMP90015 Distributed System Project1 EZServer
 * The fetch function of the server
 */
class Fetch {

    /**
     * The method to achieve server's fetch function, so that the server could
     * response to the client
     *
     * @param resource the client's resource request
     * @param db       the server's database
     * @return the resource that hit by the client's request
     * @throws InvalidResourceException  If the resource supplied contains illegal fields, this is thrown.
     * @throws MissingComponentException If the resource supplied contains missing fields, this is thrown.
     */
    static Resource fetch(JSONReader resource, HashDatabase db) throws InvalidResourceException, MissingComponentException {

        String name = resource.getResourceName();
        String description = resource.getResourceDescription();
        String channel = resource.getResourceChannel();
        String owner = resource.getResourceOwner();
        String uri = resource.getResourceUri();
        String[] tags = resource.getResourceTags();

        //Check strings etc. are valid.
        if (!Common.validateResource(name, description, tags, uri, channel, owner)) {
            throw new InvalidResourceException("Tying to remove resource with invalid fields.");
        }

        try {
            URI path = new URI(uri);
            if (path.toString().equals("")) {
                throw new MissingComponentException("missing resourceTemplate - Missing uri");
            }
            if (!path.isAbsolute() && !path.getScheme().equals("file")) {
                throw new InvalidResourceException("Trying to download resource with non-absolute or non-file uri.");
            }
        } catch (URISyntaxException e) {
            throw new InvalidResourceException("Attempting to download resource with invalid uri syntax.");
        }

        //Delete according to primary key.
        Resource match = db.pKeyLookup(channel, uri);
        if (match != null) {
            return match;
        }
        throw new InvalidResourceException("Tried to download a non-existent resource from database.");
    }
}
