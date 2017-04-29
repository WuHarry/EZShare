package EZShare;

import JSON.JSONReader;
import Resource.HashDatabase;
import Resource.Resource;
import exceptions.InvalidResourceException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Yahang Wu on 2017/4/16.
 * COMP90015 Distributed System Project1 EZServer
 * Server query function to query resources
 */
class Query {

    /**
     * The query function of the server
     *
     * @param resource The resources should be queried.
     * @param db       The database the resource should be checked.
     * @return return all the resources that match the request
     * @throws InvalidResourceException If the resource supplied contains illegal fields, this is thrown.
     */
    static Set<Resource> query(JSONReader resource, HashDatabase db) throws InvalidResourceException {
        String name = resource.getResourceName();
        String description = resource.getResourceDescription();
        String channel = resource.getResourceChannel();
        String owner = resource.getResourceOwner();
        String uri = resource.getResourceUri();
        String[] tags = resource.getResourceTags();
        boolean relay = resource.getRelay();

        Set<Resource> resources = new HashSet<Resource>();

        //Check strings etc. are valid
        if (!Common.validateResource(name, description, tags, uri, channel, owner)) {
            throw new InvalidResourceException("Trying to query Resource with illegal fields.");
        }
        if (relay) {
            String tag = "";
            for (String t : tags) {
                tag += t;
            }
            if (db.channelLookup(channel) == null) return null;
            resources.addAll(db.channelLookup(channel));
            //remove owner doesn't match
            if (!owner.equals("")) {
                resources.removeIf(resource1 -> !resource1.getOwner().equals(owner));
            }
            //remove tags doesn't match
            if (!tag.equals("")) {
                resources.removeIf(resource1 -> !Arrays.equals(resource1.getTags().toArray(), tags));
            }
            //remove uri doesn't match
            if (!uri.equals("")) {
                resources.removeIf(resource1 -> !resource1.getUri().equals(uri));
            }
            //remove name doesn't match
            if (!name.equals("")) {
                resources.removeIf(resource1 -> !resource1.getName().contains(name));
            }
            if (!description.equals("")) {
                resources.removeIf(resource1 -> !resource1.getDescription().contains(description));
            }
            hideOwner(resources);
            return resources;
        }
        return null;
    }

    /**
     * The method to hide owner, if the owner is not ""
     *
     * @param resources the response resources
     */
    private static void hideOwner(Set<Resource> resources) {
        for (Resource r : resources) {
            if (!r.getOwner().equals("")) r.setOwner("*");
        }
    }
}
