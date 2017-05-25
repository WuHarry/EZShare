package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import Exceptions.FailedServerSubscriptionException;
import Exceptions.InvalidResourceException;
import JSON.JSONReader;
import Resource.Resource;

public class SubscriptionManager implements Subscriber<Resource, JSONReader> {

	private ReadWriteLock lock;
	private List<Subscriber> subscribers;
	private Map<String, Thread> subscriberThreads;
	private Map<InetSocketAddress,ServerConnection> serverConnections;
	private List<InetSocketAddress> servers;
	private boolean isSecure;
	private List<SubscriptionService<Resource, JSONReader>> services;
	
	private class ServerConnection{
		public int subscriptions;
		public DataInputStream input;
		public DataOutputStream output;
		public Thread thread;
		public Socket socket;
		
		public ServerConnection(int subs, DataInputStream input, DataOutputStream output, Thread thread, Socket socket){
			this.subscriptions = subs;
			this.input = input;
			this.output = output;
			this.thread = thread;
			this.socket = socket;
		}
		
	}
	
	private class Subscriber{
		public String id;
		public DataInputStream in;
		public DataOutputStream out;
		public Thread listenerThread;
		public Socket socket;
		public List<JSONReader> resourceTemplates;
		
		public Subscriber(String id, DataInputStream in, DataOutputStream out, Thread listenerThread, Socket socket){
			this.id = id;
			this.in = in;
			this.out = out;
			this.listenerThread = listenerThread;
			this.socket = socket;
			this.resourceTemplates = new ArrayList<JSONReader>();
		}
		
	}
	
	public SubscriptionManager(List<InetSocketAddress> servers, boolean isSecure) {
		lock = new ReentrantReadWriteLock();
		subscribers = new ArrayList<Subscriber>();
		subscriberThreads = new HashMap<String, Thread>();
		serverConnections = new HashMap<InetSocketAddress,ServerConnection>();
		this.servers = servers;
		this.isSecure = isSecure;
		this.services = new ArrayList<SubscriptionService<Resource, JSONReader>>();
	}
	
	public void listenTo(SubscriptionService<Resource, JSONReader> db){
		lock.writeLock().lock();
		try{
			db.subscribe(this);
			this.services.add(db);
		}finally{
			lock.writeLock().unlock();
		}
	}

	public void subscribe(JSONReader newResource, Socket clientSocket, DataInputStream input, DataOutputStream output) throws InvalidResourceException, IOException{
		lock.writeLock().lock();
		try{
			if(subscriberThreads.containsKey(newResource.getSubscriptionID())){
				//TODO: Already has subscription for that id, do something
				
			}else{
				
			}
			//Start new thread for listening to unsubscribe, then returns and finishes original thread.
			Thread listenThread = new Thread(()->listenToSubscriber(clientSocket, input, output));
			Subscriber subscriber = new Subscriber(newResource.getSubscriptionID(), input, output, listenThread, clientSocket);
			subscriber.resourceTemplates.add(newResource);
			if(newResource.getRelay() == true){
				//relay to all servers in server list
				synchronized(this.servers){
					for(InetSocketAddress i: servers){
						Socket socket = null;
						try{
							DataOutputStream os = null;
							ServerConnection conn;
							Thread serverThread = null;
							if(!serverConnections.containsKey(i)){
								//Set up new server connection
								int subs = 1;
								if(this.isSecure == true){
									//create secure socket
									socket = Common.initClientSSL().createSocket(i.getHostName(), i.getPort());
								}else{
									//create insecure socket
									socket = new Socket(i.getHostName(), i.getPort());
								}
								DataInputStream in = new DataInputStream(socket.getInputStream());
								DataOutputStream out = new DataOutputStream(socket.getOutputStream());
								conn = new ServerConnection(subs, in, out, null, socket);
								serverThread = new Thread(()->listenToServer(conn));
								conn.thread = serverThread;
							}else{
								(conn = serverConnections.get(i)).subscriptions++;
								os = conn.output;
							}
							//Submit subscription request to server.
							JsonObject command = newResource.getJsonObject();
							os.writeUTF(command.toString());
							os.flush();
							String response = conn.input.readUTF();
							if(JSONReader.isJSONValid(response)){
								JsonParser parse = new JsonParser();
						        JsonObject reply = (JsonObject) parse.parse(response);
						        if((!reply.has("response")) || reply.get("response").getAsString() != "success"){
						          	//failed, throw exception.
						           	throw new FailedServerSubscriptionException();
						        }
							}else{
								throw new IOException();
							}
							if(serverThread != null){
								this.serverConnections.put(i, conn);
								serverThread.start();
							}
						}catch(IOException e1){
							if(socket != null){
								socket.close();
							}
						}catch(FailedServerSubscriptionException e2){
							if(socket != null){
								socket.close();
							}
						}
					}
				}
			}
			listenThread.start();
			subscriberThreads.put(newResource.getSubscriptionID(), listenThread);
			subscribers.add(subscriber);
			for(SubscriptionService<Resource,JSONReader> service: services){
				List<Resource> resources = service.query(newResource);
				send(subscriber.out,resources);
			}
		}finally{
			lock.writeLock().unlock();
		}
		
	}

	@Override
	public void notifySubscriber(SubscriptionService<Resource, JSONReader> subService) {
		for(Subscriber subscriber: subscribers){
			for(JSONReader template: subscriber.resourceTemplates){
				List<Resource> reply = subService.query(template);
				try {
					send(subscriber.out, reply);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void notifySubscriber(Resource obj){
		// TODO Auto-generated method stub
		for(Subscriber subscriber: subscribers){
			for(JSONReader template: subscriber.resourceTemplates){
				if(match(template, obj)){
					try {
						send(subscriber.out, obj);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void listenToSubscriber(Socket clientSocket, DataInputStream input, DataOutputStream output){
		//TODO: Listen for unsubscribe, then unsubscribe when command comes through. Listen for new Resource templates.
		
	}
	
	private void listenToServer(ServerConnection sc){
		//TODO: Listen for resources
		
	}
	
	private void send(DataOutputStream out, List<Resource> resources) throws IOException{
		for(Resource resource: resources){
			send(out, resource);
		}
	}
	
	private void send(DataOutputStream out, Resource res) throws IOException{
		out.writeUTF(res.toJsonObject().toString());
		out.flush();
	}
	
	private boolean match(JSONReader template, Resource resource){
		//TODO: true if resource matches template.
		return false;
	}

}
