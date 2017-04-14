package Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A synchronized database which allows lookup of resources based on their template,
 * by multiple threads simultaneously. 
 * 
 * Created by Ryan Naughton 15/04/2017
 *
 */
public class HashDatabase<synchronised> {

	private ReadWriteLock lock;
	private Map<String, Resource> uriMap;
	private Map<String, List<Resource>> nameMap;
	private Map<String, List<Resource>> descMap;
	private Map<String, List<Resource>> channelMap;
	private Map<String, List<Resource>> ownerMap;
	
	/**
	 * Create new HashDatabase with no Resources stored.
	 */
	public HashDatabase(){
		lock = new ReentrantReadWriteLock();
		this.uriMap = new HashMap<String, Resource>();
		this.nameMap = new HashMap<String, List<Resource>>();
		this.descMap = new HashMap<String, List<Resource>>();
		this.channelMap = new HashMap<String, List<Resource>>();
		this.ownerMap = new HashMap<String, List<Resource>>();
	}
	
	/**
	 * Returns true if the database contains a file associated with the given uri.
	 * @param uri uri to lookup.
	 * @return True if a resource is associated with the given uri, otherwise false.
	 */
	public boolean containsURI(String uri){
		lock.readLock().lock();
		try{
			return this.uriMap.containsKey(uri);
		}finally{
			lock.readLock().unlock();
		}
	}
	
	/**
	 * Returns resource matching uri or null if no resource matches the uri.
	 * @param uri uri used for lookup.
	 * @return Resource found or null if no match.
	 */
	public Resource uriLookup(String uri){
		if(uri == null){
			//Handle null uri, currently just crashes
			throw new IllegalArgumentException("Cannot lookup resource by uri when uri is null.");
		}
		lock.readLock().lock();
		try{
			if(!this.uriMap.containsKey(uri)){
				return null;
			}
			return uriMap.get(uri);
		}finally{
			lock.readLock().unlock();
		}
	}
	
	/**
	 * @param name The name of the resource to search for.
	 * @return List of resources under the given name, or null if no resources have this name.
	 */
	public List<Resource> nameLookup(String name){
		if(name == null){
			//Just crashes at the moment
			throw new IllegalArgumentException("Cannot lookup resource by name when name is null.");
		}
		lock.readLock().lock();
		try{
			if(!this.nameMap.containsKey(name)){
				return null;
			}
			return nameMap.get(name);
		}finally{
			lock.readLock().unlock();
		}
	}
	
	/**
	 * @param desc The description of the resource to search for.
	 * @return List of resources with the given description, or null if none are found.
	 */
	public List<Resource> descLookup(String desc){
		if(desc == null){
			//Just crashes at the moment
			throw new IllegalArgumentException("Cannot lookup resource by description when description is null.");
		}
		lock.readLock().lock();
		try{
			if(!this.descMap.containsKey(desc)){
				return null;
			}
			return descMap.get(desc);
		}finally{
			lock.readLock().unlock();
		}
	}
	
	/**
	 * @param channel The channel of the resource to search for.
	 * @return List of resources in the given channel, or null if none exist.
	 */
	public List<Resource> channelLookup(String channel){
		if(channel == null){
			//Just crashes at the moment
			throw new IllegalArgumentException("Cannot lookup resource by channel when channel is null.");
		}
		lock.readLock().lock();
		try{
			if(!this.channelMap.containsKey(channel)){
				return null;
			}
			return channelMap.get(channel);
		}finally{
			lock.readLock().unlock();
		}
	}
	
	/**
	 * @param owner The owner of the resource to search for.
	 * @return List of resources with given owner, or null if none exist.
	 */
	public List<Resource> ownerLookup(String owner){
		if(owner == null){
			//Just crashes at the moment
			throw new IllegalArgumentException("Cannot lookup resource by owner when owner is null.");
		}
		lock.readLock().lock();
		try{
			if(!this.ownerMap.containsKey(owner)){
				return null;
			}
			return ownerMap.get(owner);
		}finally{
			lock.readLock().unlock();
		}
	}
	
	/**
	 * Inserts the given resource into the database, updating maps appropriately.
	 * Will overwrite equal resource, but does not guarantee automatic clearing of previous 
	 * resource if equal resources have differing fields (user's responsibility).
	 * @param res The resource to be inserted.
	 */
	public void insertResource(Resource res){
		//NOTE: If resource with identical uri to prev but different other fields 
		//is inserted, will overwrite uri references but not others necessarily.
		//Use containsURI and delete to ensure correctness.
		if(res == null){
			//Just crashes at the moment
			throw new IllegalArgumentException("Cannot insert null resource into HashDatabase.");
		}
		List<Resource> temp;
		lock.writeLock().lock();
		try{
			//insert into all maps.
			this.uriMap.put(res.getUri(), res);
			if(this.channelMap.containsKey(res.getChannel())){
				temp = this.channelMap.get(res.getChannel());
				if(temp.contains(res)){
					temp.remove(res);
				}
				temp.add(res);
			}else{
				this.channelMap.put(res.getChannel(), temp = new ArrayList<Resource>());
				temp.add(res);
			}
			if(this.ownerMap.containsKey(res.getOwner())){
				temp = this.ownerMap.get(res.getOwner());
				if(temp.contains(res)){
					temp.remove(res);
				}
				temp.add(res);
			}else{
				this.ownerMap.put(res.getOwner(), temp = new ArrayList<Resource>());
				temp.add(res);
			}
			if(this.nameMap.containsKey(res.getName())){
				temp = this.nameMap.get(res.getName());
				if(temp.contains(res)){
					temp.remove(res);
				}
				temp.add(res);
			}else{
				this.nameMap.put(res.getName(), temp = new ArrayList<Resource>());
				temp.add(res);
			}
			if(this.descMap.containsKey(res.getDescription())){
				temp = this.descMap.get(res.getDescription());
				if(temp.contains(res)){
					temp.remove(res);
				}
				temp.add(res);
			}else{
				this.descMap.put(res.getDescription(), temp = new ArrayList<Resource>());
				temp.add(res);
			}
		}finally{
			lock.writeLock().unlock();
		}
	}
	
	/**
	 * Deletes all references to the given resource from the database.
	 * @param res The resource to be deleted from the database.
	 */
	public void deleteResource(Resource res){
		if(res == null){
			//Just crashes at the moment
			throw new IllegalArgumentException("Cannot delete null resource from HashDatabase.");
		}
		lock.writeLock().lock();
		try{
			//delete from all maps.
			if(this.channelMap.containsKey(res.getChannel())){
				this.channelMap.get(res.getChannel()).remove(res);
			}
			if(this.descMap.containsKey(res.getDescription())){
				this.descMap.get(res.getDescription()).remove(res);
			}
			if(this.nameMap.containsKey(res.getName())){
				this.nameMap.get(res.getName()).remove(res);
			}
			if(this.ownerMap.containsKey(res.getOwner())){
				this.ownerMap.get(res.getOwner()).remove(res);
			}
			this.uriMap.remove(res.getUri());
		}finally{
			lock.writeLock().unlock();
		}
	}
	
}
