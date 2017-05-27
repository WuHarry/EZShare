package Resource;

import Connection.Connection;
import EZShare.Common;
import EZShare.Query;
import EZShare.Server;
import EZShare.Subscriber;
import EZShare.SubscriptionService;
import Exceptions.InvalidResourceException;
import JSON.JSONReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A synchronized database which allows lookup of resources based on their template,
 * by multiple threads simultaneously.
 * Created by Ryan Naughton 15/04/2017
 */
public class HashDatabase implements SubscriptionService<Resource, JSONReader> {

    private ReadWriteLock lock;
    private ReadWriteLock subscribeLock;
    //Maps channel to map of uri to resource.
    private Map<String, ChannelDB> db;
    private int size = 0;
    private List<Subscriber<Resource, JSONReader>> subscribers;
    
    /**
     * Internal class to separate resources by channel and allow
     * lookup within a channel by all fields.
     */
    private class ChannelDB {
        private Map<String, Resource> uriMap;

        ChannelDB() {
            this.uriMap = new HashMap<String, Resource>();
        }
    }

    /**
     * Create new HashDatabase with no Resources stored.
     */
    public HashDatabase() {
        lock = new ReentrantReadWriteLock();
        subscribeLock = new ReentrantReadWriteLock();
        this.db = new HashMap<String, ChannelDB>();
        this.subscribers = new ArrayList<Subscriber<Resource, JSONReader>>();
        
        //Add a new query
        List<String> tags = new ArrayList<String>();
        tags.add("jpg");
        String ezserver = Connection.hostName + ":" + Server.port;
        insertResource(new Resource("Biubiubiu", "default",
                tags, "http://www.unimelb.edu.au", "", "Biubiubiu", ezserver));
    }

    /**
     * Looks up a resource by its primary key, returns that resource if it is present or null
     * otherwise.
     *
     * @param channel The channel of the resource to search for.
     * @param uri     The uri of the resource.
     * @return Resource which matches the key or null if none found.
     */
    public Resource pKeyLookup(String channel, String uri) {
        if (channel == null || uri == null) {
            throw new IllegalArgumentException("Cannot lookup primary key in database with null elements.");
        }
        lock.readLock().lock();
        try {
            ChannelDB channelDB;
            if (this.db.containsKey(channel)) {
                channelDB = this.db.get(channel);
                if (channelDB.uriMap.containsKey(uri)) {
                    return channelDB.uriMap.get(uri);
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns list of resources matching uri or null if no resource matches the uri.
     *
     * @param channel Channel to search in.
     * @param uri     uri used for lookup.
     * @return Resource found or null if no match.
     */
    public Resource uriLookup(String channel, String uri) {
        if (uri == null || channel == null) {
            throw new IllegalArgumentException("Cannot lookup resource by uri when uri is null.");
        }
        ChannelDB channelDB;
        lock.readLock().lock();
        try {
            if (!this.db.containsKey(channel)) {
                return null;
            }
            channelDB = this.db.get(channel);
            if (!channelDB.uriMap.containsKey(uri)) {
                return null;
            }
            return channelDB.uriMap.get(uri);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param channel The channel of the resource to search for.
     * @return List of resources in the given channel, or null if none exist.
     */
    public Collection<Resource> channelLookup(String channel) {
        if (channel == null) {
            throw new IllegalArgumentException("Cannot lookup resource by channel when channel is null.");
        }
        lock.readLock().lock();
        try {
            if (!this.db.containsKey(channel) || this.db.get(channel).uriMap.isEmpty()) {
                return null;
            }
            return this.db.get(channel).uriMap.values();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Inserts the given resource into the database, updating maps appropriately.
     * Will overwrite equal resource, but does not guarantee automatic clearing of previous
     * resource if equal resources have differing fields (user's responsibility).
     *
     * @param res The resource to be inserted.
     */
    public void insertResource(Resource res) {
        //NOTE: Use containsURI and delete to ensure correctness.
        if (res == null) {
            throw new IllegalArgumentException("Cannot insert null resource into HashDatabase.");
        }
        List<Resource> temp;
        lock.writeLock().lock();
        try {
            if (!this.db.containsKey(res.getChannel())) {
                this.db.put(res.getChannel(), new ChannelDB());
            }
            ChannelDB channelDB = this.db.get(res.getChannel());
            //insert into all maps.
            if (channelDB.uriMap.containsKey(res.getUri())) {
                //Make sure same owner, otherwise not allowed.
                if (!res.getOwner().equals(channelDB.uriMap.get(res.getUri()).getOwner())) {
                    //Handle illegal attempt at insert.
                    throw new IllegalStateException("Cannot insert resources with same uris but different owners to a channel.");
                }
            }
            channelDB.uriMap.put(res.getUri(), res);
            //update the size of the database
            size++;
        } finally {
            lock.writeLock().unlock();
        }
        subscribeLock.readLock().lock();
        try{
            for(Subscriber<Resource, JSONReader> s: subscribers){
          	  s.notifySubscriber(res);
      	    }
        }finally{
        	subscribeLock.readLock().unlock();
        }
    }

    /**
     * Deletes all references to the given resource from the database.
     *
     * @param res The resource to be deleted from the database.
     */
    public void deleteResource(Resource res) {
        if (res == null) {
            throw new IllegalArgumentException("Cannot delete null resource from HashDatabase.");
        }
        ChannelDB channelDB;
        lock.writeLock().lock();
        try {
            //delete from all maps.
            if (this.db.containsKey(res.getChannel())) {
                channelDB = this.db.get(res.getChannel());
                channelDB.uriMap.remove(res.getUri());
                //update the size of the database
                size--;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * The method to get the size of the database
     *
     * @return the number of resources
     */
    public int getDatabaseSize() {
        return this.size;
    }

	@Override
	public void subscribe(Subscriber<Resource, JSONReader> subscriber) {
		subscribeLock.writeLock().lock();
		try{
			subscribers.add(subscriber);
		}finally{
			subscribeLock.writeLock().unlock();
		}
		subscriber.notifySubscriber(this);		
	}

	@Override
	public List<Resource> query(JSONReader resource) throws InvalidResourceException {
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

        String tag = "";
        for (String t : tags) {
            tag += t;
        }
        if (channelLookup(channel) == null) return null;
        resources.addAll(channelLookup(channel));
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
        //remove description doesn't match
        if (!description.equals("")) {
            resources.removeIf(resource1 -> !resource1.getDescription().contains(description));
        }

        Query.hideOwner(resources);
        return new ArrayList<Resource>(resources);
	}
}
