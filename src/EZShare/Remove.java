package EZShare;

import java.net.URI;
import java.net.URISyntaxException;

import JSON.JSONReader;
import Resource.HashDatabase;
import Resource.Resource;
import exceptions.InvalidResourceException;

/**
 * 
 * @author Danni Zhao
 * 17 April 2017
 * remove the resource with the primary key;
 * The other fields of the resource are not needed; if they exist, ignore;
 *
 */
public class Remove {
	public static void remove (JSONReader resource, HashDatabase db) throws InvalidResourceException {
		
	    String channel = resource.getResourceChannel();
	    String uri = resource.getResourceUri();	    
        try {
            URI path = new URI(uri);
            if (!path.isAbsolute() || path.getScheme().equals("file")) {
                throw new InvalidResourceException("Trying to publish resource with non-absolute or file uri.");
            }
        } catch (URISyntaxException e) {
            throw new InvalidResourceException("Attempting to publish resource with invalid uri syntax.");
        }

        //need debug the channel content
        Resource match = db.uriLookup(channel, uri);
        if (match != null) {
            db.deleteResource(match);
        }

	}
}
