package EZShare;

import JSON.JSONReader;
import Resource.HashDatabase;
import Resource.Resource;
import exceptions.InvalidResourceException;
import exceptions.NonExistentResourceException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Yahang Wu on 2017/4/24.
 * COMP90015 Distributed System Project1 EZServer
 * The fetch function of the server
 */
public class Fetch {

    static Resource fetch(JSONReader resource, HashDatabase db) throws InvalidResourceException {

        String name = resource.getResourceName();
        String description = resource.getResourceDescription();
        String channel = resource.getResourceChannel();
        String owner = resource.getResourceOwner();
        String uri = resource.getResourceUri();
        String[] tags = resource.getResourceTags();

        //Check strings etc. are valid.
        if(!Common.validateResource(name, description, tags, uri, channel, owner)){
            throw new InvalidResourceException("Tying to remove resource with invalid fields.");
        }

        try{
            URI path = new URI(uri);
            if (!path.isAbsolute() && !path.getScheme().equals("file")){
                throw new InvalidResourceException("Trying to download resource with non-absolute or non-file uri.");
            }
        }catch (URISyntaxException e) {
            throw new InvalidResourceException("Attempting to download resource with invalid uri syntax.");
        }

        //Delete according to primary key.
        Resource match = db.pKeyLookup(channel, uri);
        if(match!=null){
            return match;
        }
        throw new InvalidResourceException("Tried to download a non-existent resource from database.");
    }
}
