package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import Exceptions.FailedServerSubscriptionException;
import Exceptions.InvalidResourceException;
import Exceptions.MissingComponentException;
import JSON.JSONReader;
import Resource.Resource;

/**
 * Manages subscriptions of clients and relaying to servers. Handles concurrent accesses.
 *
 */
public class SubscriptionManager implements Subscriber<Resource, JSONReader> {

	private static final String SUBSCRIBE = "SUBSCRIBE";
	private static final String UNSUBSCRIBE = "UNSUBSCRIBE";
	
	private ReadWriteLock lock;
	private List<Subscriber> subscribers;
	private Map<String, Thread> subscriberThreads;
	private Map<InetSocketAddress,ServerConnection> serverConnections;
	private List<InetSocketAddress> servers;
	private boolean isSecure;
	private List<SubscriptionService<Resource, JSONReader>> services;
	
	/**
	 * Encapsulates connection to server via a relayed subscription, which may send
	 * hits to the SubscriptionManager.
	 *
	 */
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
	
	/**
	 * Encapsulates connection to a subscriber which may send new subscriptions, and be transmitted 'hits'.
	 *
	 */
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
	
	/**
	 * Create a new SubscriptionManager with a list of servers used for relay and a flag indicating
	 * whether SSL or insecure connections are used.
	 * @param servers List of servers to be relayed to.
	 * @param isSecure Flag which indicates whether connections are SSL or not.
	 */
	public SubscriptionManager(List<InetSocketAddress> servers, boolean isSecure) {
		lock = new ReentrantReadWriteLock();
		subscribers = new ArrayList<Subscriber>();
		subscriberThreads = new HashMap<String, Thread>();
		serverConnections = new HashMap<InetSocketAddress,ServerConnection>();
		this.servers = servers;
		this.isSecure = isSecure;
		this.services = new ArrayList<SubscriptionService<Resource, JSONReader>>();
	}
	
	/**
	 * Adds db as a database SubscriptionManager is listening to.
	 * @param db The SubscriptionService this SubjectManager should listen to.
	 */
	public void listenTo(SubscriptionService<Resource, JSONReader> db){
		lock.writeLock().lock();
		try{
			db.subscribe(this);
			this.services.add(db);
		}finally{
			lock.writeLock().unlock();
		}
	}

	/**
	 * Subscribe the client with given details to receive any hits which match newResource's template.
	 * @param newResource The resource template as a JSONReader object.
	 * @param clientSocket The socket of the client subscribing.
	 * @param input The DataInputStream of clientSocket. 
	 * @param output The DataOutputStream of clientSocket.
	 * @throws InvalidResourceException Resource template is invalid.
	 * @throws IOException Networking error.
	 */
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
						openConnection(i, newResource);
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
		lock.writeLock().lock();
		try{
			for(Subscriber subscriber: subscribers){
				for(JSONReader template: subscriber.resourceTemplates){
					List<Resource> reply = Collections.emptyList();
					try {
						reply = subService.query(template);
					} catch (InvalidResourceException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
						send(subscriber.out, reply);
					} catch (IOException e) {
						// TODO Auto-generated catch block, if exception connection broken.
						e.printStackTrace();
					}
				}
			}
		}finally{
			lock.writeLock().unlock();
		}
	}

	@Override
	public void notifySubscriber(Resource obj){
		lock.writeLock().lock();
		try{
			for(Subscriber subscriber: subscribers){
				for(JSONReader template: subscriber.resourceTemplates){
					if(match(template, obj)){
						try {
							send(subscriber.out, obj);
						} catch (IOException e) {
							//TODO: Connection broken, should do something.
							e.printStackTrace();
						}
					}
				}
			}
		}finally{
			lock.writeLock().unlock();
		}
	}
	
	/**
	 * TODO: Could probably just pass Subscriber.
	 * Listen on subscriber socket for new messages (unsubscribe or new subscriptions).
	 * @param clientSocket The socket subscriber uses.
	 * @param input DataInputStream for this connection, in from subscriber.
	 * @param output DataOutputStream for this connection, out to subscriber.
	 */
	private void listenToSubscriber(Socket clientSocket, DataInputStream input, DataOutputStream output){
		//TODO: Listen for unsubscribe, then unsubscribe when command comes through. Listen for new Resource templates.
		boolean running = true;
		while(running){
			try{
				String message = input.readUTF();
				if(JSONReader.isJSONValid(message)){
					JSONReader command = new JSONReader(message);
					switch(command.getCommand()){
					case SUBSCRIBE:
						Common.checkNull(command);
						String id;
						if((id = command.getSubscriptionID()) == null){
							throw new MissingComponentException("Missing ID");
						}
						this.subscribe(command, clientSocket, input, output);
						JsonObject reply = new JsonObject();
						reply.addProperty("response", "success");
						reply.addProperty("id", id);
						Server.logger.info("Subscribed new client: " + id + ".");
						output.writeUTF(reply.toString());
                		Server.logger.fine("[SENT] - " + reply.toString());
						break;
					case UNSUBSCRIBE:
						running = false;
						break;
					default:
						break;
					}
				}
			}catch(IOException e1){
				
			} catch (MissingComponentException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (InvalidResourceException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			}
		}
	}
	
	/**
	 * Listen to a server for new hits.
	 * @param sc The connection object to this server.
	 */
	private void listenToServer(ServerConnection sc){
		//TODO: Listen for resources
		boolean running = true;
		while(running){
			try{
				String message = sc.input.readUTF();
				if(JSONReader.isJSONValid(message)){
					JSONReader resource = new JSONReader(message);
					if(resource.getResources() != null){
						//Resource present, read in resource and notify to find client/s interested.
						Common.checkNull(resource);
						Resource newResource = new Resource(resource.getResourceName(), resource.getResourceDescription(),
								                            Arrays.asList(resource.getResourceTags()),
								                            resource.getResourceUri(), resource.getResourceChannel(),
								                            resource.getResourceOwner(), resource.getResourceEZserver());
						this.notifySubscriber(newResource);
					}
				}
			}catch(IOException e1){
				e1.printStackTrace();
			} catch (MissingComponentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Sends all resources in list on out stream.
	 * @param out The stream all resources are output to.
	 * @param resources The resources to be transmitted.
	 * @throws IOException If an I/O error occurs.
	 */
	private void send(DataOutputStream out, List<Resource> resources) throws IOException{
		for(Resource resource: resources){
			send(out, resource);
		}
	}
	
	/**
	 * Send a single resource on out stream.
	 * @param out Stream resource output to.
	 * @param res Resource to be transmitted.
	 * @throws IOException If an I/O error occurs.
	 */
	private void send(DataOutputStream out, Resource res) throws IOException{
		out.writeUTF(res.toJsonObject().toString());
		out.flush();
	}
	
	/**
	 * Returns true if resource matches the template, otherwise false.
	 * @param template Resource template to be compared to resource.
	 * @param resource The actual resource to determine if described by template.
	 * @return True if template describes resource, false otherwise.
	 */
	private boolean match(JSONReader template, Resource resource){
		//TODO: true if resource matches template.
		return false;
	}

	private void openConnection(InetSocketAddress server, JSONReader newResource) throws IOException{
		Socket socket = null;
		try{
			DataOutputStream os = null;
			ServerConnection conn;
			Thread serverThread = null;
			if(!serverConnections.containsKey(server)){
				//Set up new server connection
				int subs = 1;
				if(this.isSecure == true){
					//create secure socket
					socket = Common.initClientSSL().createSocket(server.getHostName(), server.getPort());
				}else{
					//create insecure socket
					socket = new Socket(server.getHostName(), server.getPort());
				}
				DataInputStream in = new DataInputStream(socket.getInputStream());
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				conn = new ServerConnection(subs, in, out, null, socket);
				serverThread = new Thread(()->listenToServer(conn));
				conn.thread = serverThread;
			}else{
				(conn = serverConnections.get(server)).subscriptions++;
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
				this.serverConnections.put(server, conn);
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
