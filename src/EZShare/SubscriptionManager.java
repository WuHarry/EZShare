package EZShare;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.JsonObject;

import Exceptions.InvalidResourceException;
import JSON.JSONReader;
import Resource.Resource;

public class SubscriptionManager implements Subscriber<Resource> {

	private ReadWriteLock lock;
	private List<Pair<String,JsonObject>> subscribers;
	
	public SubscriptionManager() {
		lock = new ReentrantReadWriteLock();
		subscribers = new ArrayList<Pair<String,JsonObject>>();
	}
	
	public void listenTo(SubscriptionService<Resource> db){
		db.subscribe(this);
	}

	public void subscribe(JSONReader newResource, Socket clientSocket) throws InvalidResourceException{
		// TODO Auto-generated method stub
		lock.writeLock().lock();
		Pair<String, JsonObject> subscriber = new Pair<String,JsonObject>(newResource.getSubscriptionID(), newResource.getResourceTemplate());
		subscribers.add(subscriber);
	}

	@Override
	public void notifySubscriber(SubscriptionService<Resource> subService) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifySubscriber(Resource obj) {
		// TODO Auto-generated method stub
		
	}
	
	

}
