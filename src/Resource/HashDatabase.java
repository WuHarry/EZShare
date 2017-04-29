package Resource;

import Connection.Connection;
import EZShare.Server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A synchronized database which allows lookup of resources based on their template,
 * by multiple threads simultaneously.
 * Created by Ryan Naughton 15/04/2017
 */

public class HashDatabase {

    private ReadWriteLock lock;
    //Maps channel to map of uri to resource.
    private Map<String, ChannelDB> db;
    private int size = 0;

    /**
     * Internal class to separate resources by channel and allow
     * lookup within a channel by all fields.
     */
    private class ChannelDB {
        private Map<String, Resource> uriMap;
        private Map<String, List<Resource>> nameMap;
        private Map<String, List<Resource>> descMap;
        private Map<String, List<Resource>> ownerMap;

        public ChannelDB() {
            this.uriMap = new HashMap<String, Resource>();
            this.nameMap = new HashMap<String, List<Resource>>();
            this.descMap = new HashMap<String, List<Resource>>();
            this.ownerMap = new HashMap<String, List<Resource>>();
        }
    }

    /**
     * Create new HashDatabase with no Resources stored.
     */
    public HashDatabase() {
        lock = new ReentrantReadWriteLock();
        this.db = new HashMap<String, ChannelDB>();
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
     * @param channel Channel to search in.
     * @param name    The name of the resource to search for.
     * @return Collection of resources under the given name, or null if no resources have this name.
     */
    public Collection<Resource> nameLookup(String channel, String name) {
        if (name == null || channel == null) {
            throw new IllegalArgumentException("Cannot lookup resource by name when name is null.");
        }
        ChannelDB channelDB;
        lock.readLock().lock();
        try {
            if (!this.db.containsKey(channel)) {
                return null;
            }
            channelDB = this.db.get(channel);
            if (!channelDB.nameMap.containsKey(name) || channelDB.nameMap.get(name).isEmpty()) {
                return null;
            }
            return channelDB.nameMap.get(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param channel Channel to search in.
     * @param desc    The description of the resource to search for.
     * @return List of resources with the given description, or null if none are found.
     */
    public Collection<Resource> descLookup(String channel, String desc) {
        if (desc == null || channel == null) {
            throw new IllegalArgumentException("Cannot lookup resource by description when description is null.");
        }
        ChannelDB channelDB;
        lock.readLock().lock();
        try {
            if (!this.db.containsKey(channel)) {
                return null;
            }
            channelDB = this.db.get(channel);
            if (!channelDB.descMap.containsKey(desc) || channelDB.descMap.get(desc).isEmpty()) {
                return null;
            }
            return channelDB.descMap.get(desc);
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
     * @param channel Channel to search in.
     * @param owner   The owner of the resource to search for.
     * @return List of resources with given owner, or null if none exist.
     */
    public Collection<Resource> ownerLookup(String channel, String owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Cannot lookup resource by owner when owner is null.");
        }
        ChannelDB channelDB;
        lock.readLock().lock();
        try {
            if (!this.db.containsKey(channel)) {
                return null;
            }
            channelDB = this.db.get(channel);
            if (!channelDB.ownerMap.containsKey(owner) || channelDB.ownerMap.get(owner).isEmpty()) {
                return null;
            }
            return channelDB.ownerMap.get(owner);
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
            if (channelDB.ownerMap.containsKey(res.getOwner())) {
                temp = channelDB.ownerMap.get(res.getOwner());
                if (temp.contains(res)) {
                    temp.remove(res);
                }
                temp.add(res);
            } else {
                channelDB.ownerMap.put(res.getOwner(), temp = new ArrayList<Resource>());
                temp.add(res);
            }
            if (channelDB.nameMap.containsKey(res.getName())) {
                temp = channelDB.nameMap.get(res.getName());
                if (temp.contains(res)) {
                    temp.remove(res);
                }
                temp.add(res);
            } else {
                channelDB.nameMap.put(res.getName(), temp = new ArrayList<Resource>());
                temp.add(res);
            }
            if (channelDB.descMap.containsKey(res.getDescription())) {
                temp = channelDB.descMap.get(res.getDescription());
                if (temp.contains(res)) {
                    temp.remove(res);
                }
                temp.add(res);
            } else {
                channelDB.descMap.put(res.getDescription(), temp = new ArrayList<Resource>());
                temp.add(res);
            }
        } finally {
            lock.writeLock().unlock();
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
                if (channelDB.descMap.containsKey(res.getDescription())) {
                    channelDB.descMap.get(res.getDescription()).remove(res);
                }
                if (channelDB.nameMap.containsKey(res.getName())) {
                    channelDB.nameMap.get(res.getName()).remove(res);
                }
                if (channelDB.ownerMap.containsKey(res.getOwner())) {
                    channelDB.ownerMap.get(res.getOwner()).remove(res);
                }
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
}
