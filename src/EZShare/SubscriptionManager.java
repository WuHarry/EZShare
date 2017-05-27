package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
import com.sun.media.jfxmedia.logging.Logger;

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
	private Map<Pair<String,Socket>, Subscriber> subscriberMap;
	private Map<Socket, Integer> connections;
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
		public int hits;
		public boolean relay;
		
		public Subscriber(String id, DataInputStream in, DataOutputStream out, Thread listenerThread, Socket socket, boolean relay){
			this.id = id;
			this.in = in;
			this.out = out;
			this.listenerThread = listenerThread;
			this.socket = socket;
			this.resourceTemplates = new ArrayList<JSONReader>();
			this.hits = 0;
			this.relay = relay;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((socket == null) ? 0 : socket.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Subscriber other = (Subscriber) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (socket == null) {
				if (other.socket != null)
					return false;
			} else if (socket != other.socket)
				return false;
			return true;
		}

		private SubscriptionManager getOuterType() {
			return SubscriptionManager.this;
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
		subscriberMap = new HashMap<Pair<String,Socket>, Subscriber>();
		serverConnections = new HashMap<InetSocketAddress,ServerConnection>();
		this.servers = servers;
		this.isSecure = isSecure;
		this.services = new ArrayList<SubscriptionService<Resource, JSONReader>>();
		this.connections = new HashMap<Socket,Integer>();
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
	
	public void newServer(InetSocketAddress server){
		for(Subscriber subscriber: subscribers){
			if(subscriber.relay == true){
				for(JSONReader template: subscriber.resourceTemplates)
					try {
						this.openConnection(server, template);
					} catch (IOException e) {
						Server.logger.warning("Could not connect to server");
					}
			}
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
			Thread listenThread = null;
			Subscriber subscriber = null;
			if(subscriberMap.containsKey(newResource.getSubscriptionID()) && subscriberMap.get(newResource.getSubscriptionID()).socket == clientSocket){
				subscriber = subscriberMap.get(newResource.getSubscriptionID());
			}else{
				subscriber = new Subscriber(newResource.getSubscriptionID(), input, output, listenThread, clientSocket, newResource.getRelay());
				subscriberMap.put(new Pair<String,Socket>(newResource.getSubscriptionID(), clientSocket), subscriber);
			}
			if(!(this.connections.containsKey(clientSocket) && this.connections.get(clientSocket) > 0)){
				//Start new thread for listening to unsubscribe, then returns and finishes original thread.
				listenThread = new Thread(()->listenToSubscriber(clientSocket, input, output));
			}
			subscriber.resourceTemplates.add(newResource);
			if(newResource.getRelay() == true){
				//relay to all servers in server list
				Server.logger.fine("Relaying subscription to servers.");
				synchronized(this.servers){
					for(InetSocketAddress i: servers){
						openConnection(i, newResource);
					}
				}
			}
			if(listenThread != null){
				listenThread.start();
			}
			if(!connections.containsKey(clientSocket)){
				connections.put(clientSocket, 1);
			}else{
				int count = connections.get(clientSocket);
				connections.put(clientSocket, count+1);
			}
			subscribers.add(subscriber);
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
						send(subscriber, reply);
					} catch (InvalidResourceException e1) {
						JsonObject errorMessage = new JsonObject();
						errorMessage.addProperty("response", "error");
						errorMessage.addProperty("errorMessage", "invalid resourceTemplate");
						try{
							subscriber.out.writeUTF(errorMessage.toString());
							subscriber.out.flush();
						}catch(IOException e3){
							unsubscribe(subscriber.id, subscriber.socket);
						}
					}catch (IOException e2) {
						unsubscribe(subscriber.id, subscriber.socket);
					}
				}
			}
		}finally{
			lock.writeLock().unlock();
		}
	}

	@Override
	public void notifySubscriber(Resource obj){
		Server.logger.fine("Notifying subscription manager of new Resource.");
		lock.writeLock().lock();
		try{
			for(Subscriber subscriber: subscribers){
				for(JSONReader template: subscriber.resourceTemplates){
					if(match(template, obj)){
						try {
							send(subscriber, obj);
						} catch (IOException e) {
							this.unsubscribe(subscriber.id, subscriber.socket);
						}
					}
				}
			}
		}finally{
			lock.writeLock().unlock();
		}
	}
	
	private boolean unsubscribe(String id, Socket socket){
		boolean endThread = false;
		Server.logger.fine("Unsubscribing " + id + " from subscription manager.");
		if(this.subscriberMap.containsKey(new Pair<String, Socket>(id,socket))){
			//Subscriber exists, remove it and close connections if necessary.
			Subscriber subscriber = null;
			for(Subscriber sub: subscribers){
				if(sub.id.equals(id) && sub.socket == socket){
					subscriber = sub;
					break;
				}
			}
			if(subscriber == null){
				//Should never happen, throw exception.
				throw new IllegalStateException("SubscriptionManager corrupted (subscriber exists in map but not list).");
			}
			//Remove subscriber
			subscribers.remove(subscriber);
			//Check if thread & connection should end, do it if necessary
			int conn = connections.get(socket);
			connections.put(socket, --conn);
			if(conn == 0){
				connections.remove(socket);
				this.subscriberMap.remove(new Pair<String,Socket>(id, socket));
				endThread = true;
			}
			//Send message
			JsonObject output = new JsonObject();
			output.addProperty("resultSize", subscriber.hits);
			try{
				subscriber.out.writeUTF(output.toString());
				subscriber.out.flush();
			}catch(IOException e1){
				Server.logger.warning("Could not send unsubscription acknowledgement to subscriber.");
			}
		}
		Server.logger.fine("Unsubscribed subscriber.");
		return endThread;
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
		Server.logger.fine("Starting thread to listen to subscriber.");
		boolean running = true;
		while(running){
			try{
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
	                		output.flush();
							break;
						case UNSUBSCRIBE:
							if((id = command.getSubscriptionID()) == null){
								throw new MissingComponentException("Missing ID");
							}
							running = this.unsubscribe(id, clientSocket);
							break;
						default:
							break;
						}
					}
				}catch(SocketTimeoutException e1){
					//Do nothing and continue reading after timeout
				} catch (MissingComponentException e2) {
					if(e2.getMessage().equals("Missing ID")){
						//missing id
	        			JsonObject errorMessage = new JsonObject();
	            		errorMessage.addProperty("response", "error");
	            		errorMessage.addProperty("errorMessage", "missing resourceTemplate");
	            		output.writeUTF(errorMessage.toString());
	            		output.flush();
					}else{
						//missing resource
	        			JsonObject errorMessage = new JsonObject();
	            		errorMessage.addProperty("response", "error");
	            		errorMessage.addProperty("errorMessage", "missing resourceTemplate");
	            		output.writeUTF(errorMessage.toString());
	            		output.flush();
					}
				} catch (InvalidResourceException e3) {
					JsonObject errorMessage = new JsonObject();
	        		errorMessage.addProperty("response", "error");
	        		errorMessage.addProperty("errorMessage", "invalid resourceTemplate");
	        		output.writeUTF(errorMessage.toString());
	        		output.flush();
				} catch(IOException e4){
					Server.logger.warning("Lost Connection with client.");
					running = false;
				}
			}catch(IOException e5){
				//Means connection lost when error message sent
				Server.logger.warning("Lost Connection with client.");
				running = false;
			}
		}
	}
	
	/**
	 * Listen to a server for new hits.
	 * @param sc The connection object to this server.
	 */
	private void listenToServer(ServerConnection sc){
		//TODO: Listen for resources
		Server.logger.fine("Starting thread to listen to Server for subscription results.");
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
				Server.logger.warning("Lost connection to client.");
			} catch (MissingComponentException e) {
				// Server sent bad resource, do nothing.
			}
		}
	}
	
	/**
	 * Sends all resources in list on out stream.
	 * @param resources The resources to be transmitted.
	 * @throws IOException If an I/O error occurs.
	 */
	private void send(Subscriber subscriber, List<Resource> resources) throws IOException{
		for(Resource resource: resources){
			send(subscriber, resource);
		}
	}
	
	/**
	 * Send a single resource on out stream.
	 * @param res Resource to be transmitted.
	 * @throws IOException If an I/O error occurs.
	 */
	private void send(Subscriber subscriber, Resource res) throws IOException{
		Server.logger.fine("Sending resource to subscriber " + subscriber.id + ".");
		subscriber.out.writeUTF(res.toJsonObject().toString());
		subscriber.out.flush();
		subscriber.hits++;
	}
	
	/**
	 * Returns true if resource matches the template, otherwise false.
	 * @param template Resource template to be compared to resource.
	 * @param resource The actual resource to determine if described by template.
	 * @return True if template describes resource, false otherwise.
	 */
	private boolean match(JSONReader template, Resource resource){
		Server.logger.fine("Checking match of " + template.getJsonObject().toString() + " and " + "Name: " + resource.getName() + ", Owner: " + resource.getOwner() + ", Channel: " + resource.getChannel() + ", Description: " + resource.getDescription() + ", Tags: " + resource.getTags());
		if(!template.getResourceChannel().equals(resource.getChannel())){
			return false;
		}
		if(!template.getResourceOwner().equals(resource.getOwner()) && !(template.getResourceOwner() == "")){
			return false;
		}
		for(String templateTag: template.getResourceTags()){
			for(String tag: resource.getTags()){
				if(!tag.equals(templateTag)){
					return false;
				}
			}
		}
		if(!template.getResourceUri().equals("") && !template.getResourceUri().equals(resource.getUri())){
			return false;
		}
		if(!template.getResourceName().equals("") && !resource.getName().contains(template.getResourceName()) && 
		   !template.getResourceDescription().equals("") && !resource.getDescription().contains(template.getResourceDescription())){
			System.out.println("5");
			return false;
		}
		return true;
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
