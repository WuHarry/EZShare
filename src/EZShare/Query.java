package EZShare;

import JSON.JSONReader;
import Resource.HashDatabase;
import Resource.Resource;
import exceptions.InvalidResourceException;

import java.util.Collection;
import java.util.HashSet;


/**
 * Created by Yahang Wu on 2017/4/16.
 * COMP90015 Distributed System Project1 EZServer
 * Server query function to query resources
 */
public class Query {

    static void query(JSONReader resource, HashDatabase db) throws InvalidResourceException {
        String name = resource.getResourceName();
        String description = resource.getResourceDescription();
        String channel = resource.getResourceChannel();
        String owner = resource.getResourceOwner();
        String uri = resource.getResourceUri();
        String[] tags = resource.getResourceTags();
        String ezserver = resource.getResourceEZserver();

        HashSet<Resource> resources = new HashSet<Resource>();

        //Check strings etc. are valid
        if (!Common.validateResource(name, description, tags, uri, channel, owner)) {
            throw new InvalidResourceException("Trying to query Resource with illegal fields.");
        }
        
    }
}
