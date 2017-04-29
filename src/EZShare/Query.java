package EZShare;

import JSON.JSONReader;
import Resource.HashDatabase;
import Resource.Resource;
import exceptions.InvalidResourceException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
            //Only channel and all ""
            if ((channel + owner + name + uri + description + tag).equals(channel)) {
                if (db.channelLookup(channel) == null) return null;
                resources.addAll(db.channelLookup(channel));
                hideOwner(resources);
                return resources;
            }
            //channel and owner
            if (!owner.equals("") && (name + uri + description + tag).equals("")) {
                if (db.ownerLookup(channel, owner) == null) return null;
                resources.addAll(db.ownerLookup(channel, owner));
                hideOwner(resources);
                return resources;
            }
            //channel owner and tags
            if (!owner.equals("") && !tag.equals("") && (name + description + uri).equals("")) {
                if (db.ownerLookup(channel, owner) == null) return null;
                resources.addAll(db.ownerLookup(channel, owner));
                for (Resource r : resources) {
                    if (!Arrays.equals(r.getTags().toArray(), tags)) {
                        resources.remove(r);
                    }
                }
                hideOwner(resources);
                return resources;
            }
            //channel owner tags and uri or (name and description are "")
            if (!owner.equals("") && !tag.equals("") && !uri.equals("") && (name + description).equals("")) {
                Resource temp = db.pKeyLookup(channel, uri);
                if (temp == null) return null;
                if (temp.getOwner().equals(owner)) {
                    resources.add(temp);
                    hideOwner(resources);
                    return resources;
                }
            }
            //channel owner tags and uri or (name is not "")
            if (!owner.equals("") && !tag.equals("") && !uri.equals("") && !name.equals("") && description.equals("")) {
                Resource temp = db.pKeyLookup(channel, uri);
                if (temp == null) return null;
                if (temp.getOwner().equals(owner) && temp.getName().contains(name)) {
                    resources.add(temp);
                    hideOwner(resources);
                    return resources;
                }
            }
            //channel owner tags and uri or (description is not "")
            if (!owner.equals("") && !tag.equals("") && !uri.equals("") && name.equals("") && !description.equals("")) {
                Resource temp = db.pKeyLookup(channel, uri);
                if (temp == null) return null;
                if (temp.getOwner().equals(owner) && temp.getDescription().contains(description)) {
                    resources.add(temp);
                    hideOwner(resources);
                    return resources;
                }
            }
            //non ""
            if (!owner.equals("") && !tag.equals("") && !uri.equals("") && !name.equals("") && !description.equals("")) {
                Resource temp = db.pKeyLookup(channel, uri);
                if (temp == null) return null;
                if (temp.getOwner().equals(owner) && temp.getDescription().contains(description)
                        && temp.getName().contains(name)) {
                    resources.add(temp);
                    hideOwner(resources);
                    return resources;
                }
            }
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
