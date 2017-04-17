package EZShare;

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
class Remove {
	static void remove(JSONReader resource, HashDatabase db) throws InvalidResourceException {
		
	    String channel = resource.getResourceChannel();
	    String uri = resource.getResourceUri();

        //valid uri
	    Common.validUri(uri, "remove");

        //need debug the channel content
        Resource match = db.uriLookup(channel, uri);
        if (match != null) {
            db.deleteResource(match);
        }
	}
}
