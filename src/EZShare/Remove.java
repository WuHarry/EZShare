package EZShare;

import java.net.URI;
import java.net.URISyntaxException;

import JSON.JSONReader;
import Resource.HashDatabase;
import Resource.Resource;
import exceptions.InvalidResourceException;
import exceptions.NonExistentResourceException;

/**
 * @author Danni Zhao
 *         17 April 2017
 *         remove the resource with the primary key;
 *         The other fields of the resource are not needed; if they exist, ignore;
 */
class Remove {

    /**
     * The server's remove to read the client's request, and remove the resource
     * when the resource hit the request
     *
     * @param resource the client's resource request
     * @param db       the server's database
     * @throws InvalidResourceException     If the resource supplied contains illegal fields, this is thrown.
     * @throws NonExistentResourceException If the resource supplied contains missing fields, this is thrown.
     */
    static void remove(JSONReader resource, HashDatabase db) throws InvalidResourceException, NonExistentResourceException {

        String channel = resource.getResourceChannel();
        String uri = resource.getResourceUri();
        String name = resource.getResourceName();
        String desc = resource.getResourceDescription();
        String owner = resource.getResourceOwner();
        String[] tags = resource.getResourceTags();

        //Check strings etc. are valid.
        if (!Common.validateResource(name, desc, tags, uri, channel, owner)) {
            throw new InvalidResourceException("Tying to remove resource with invalid fields.");
        }

        //validate uri
        try {
            URI path = new URI(uri);
            if (!path.isAbsolute()) {
                throw new InvalidResourceException("Trying to remove resource with non-absolute uri.");
            }
        } catch (URISyntaxException e) {
            throw new InvalidResourceException("Attempting to remove resource with invalid uri syntax.");
        }

        //Delete according to primary key.
        Resource match = db.pKeyLookup(channel, uri);
        if (match != null && match.getOwner().equals(owner)) {
            db.deleteResource(match);
            return;
        }
        throw new NonExistentResourceException("Tried to remove a non-existent resource from database.");
    }
}
