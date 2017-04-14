package EZShare;

import JSON.JSONReader;
import Resource.HashDatabase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Yahang Wu on 2017/4/11.
 * COMP90015 Distributed System Project1 EZServer
 * The class to control the server
 * provide the method to read the request from the client
 * and write the response to the client
 */
class ServerControl {

	private static final String PUBLISH = "PUBLISH";
	private static final String REMOVE = "REMOVE";
	private static final String SHARE = "SHARE";
	private static final String QUERY = "QUERY";
	private static final String FETCH = "FETCH";
	private static final String EXCHANGE = "EXCHANGE";
	
	private static final HashDatabase db = new HashDatabase();
	
    private static Logger logger = Logger.getLogger(
            ServerControl.class.getName());

    /**
     * The method to read the clients' requests and send responses
     * @param client the socket client which is trying to connect to the server
     */
    static void serverClient(Socket client) {
        try (Socket clientSocket = client) {
        	JSONReader curr;
            String command;
            String name, description, channel, owner, uri, ezServer;
            String[] tags;
        	//input stream
            DataInputStream input =
                    new DataInputStream(clientSocket.getInputStream());
            //output stream
            DataOutputStream output =
                    new DataOutputStream(clientSocket.getOutputStream());

            String jsonString = "";
            if(input.available() != 0){
                jsonString = input.readUTF();
                if (JSONReader.isJSONValid(jsonString)){
                    //Read command from json.
                	curr = new JSONReader(jsonString);
                	command = curr.getCommand();
                	if(command == PUBLISH){
                		//publish
                		checkNull(name = curr.getResourceName());
                		checkNull(description = curr.getResourceDescription());
                		checkNull(channel = curr.getResourceChannel());
                		checkNull(owner = curr.getResourceOwner());
                		checkNull(uri = curr.getResourceUri());
                		checkNull(tags = curr.getResourceTags());
                		checkNull(ezServer = curr.getResourceEZserver());
                		publish(name, tags, description, uri, channel, owner, ezServer, db);
                	}else if(command == REMOVE){
                		checkNull(name = curr.getResourceName());
                		checkNull(description = curr.getResourceDescription());
                		checkNull(channel = curr.getResourceChannel());
                		checkNull(owner = curr.getResourceOwner());
                		checkNull(uri = curr.getResourceUri());
                		checkNull(tags = curr.getResourceTags());
                		checkNull(ezServer = curr.getResourceEZserver());
                		remove(name, tags, description, uri, channel, owner, ezServer, db);
                	}else if(command == SHARE){
                		//share
                	}else if(command == QUERY){
                		//query
                	}else if(command == FETCH){
                		//fetch
                	}else if(command == EXCHANGE){
                		//exchange
                	}else{
                		//invalid command
                	}
                	logger.fine("[RECEIVE] - " + jsonString);
                    logger.fine("[SENT] - " + "{\"response\":\"success\"}");
                    output.writeUTF("{\"response\":\"success\"}");
                    output.flush();
                }else{
                    logger.fine("[RECEIVE] - " + jsonString);
                    logger.fine("[SENT] - " + "{\"response\":\"error, invalid string\"}");
                    output.writeUTF("{\"response\":\"error, invalid string\"}");
                    output.flush();
                }
            } else {
                logger.warning("invalid request");
                output.writeUTF("{\"response\":\"error, invalid request\"}");
                output.flush();
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }
    
    private static void publish(String rName, String[] rTags, String rDesc, String rUri, 
    		                    String rChannel, String rOwner, String ezServer, HashDatabase db){
    	//Check strings etc. are valid
    	
    	//Make sure matching primary key resources are removed.
    	
    	//Add resource to database.
    }
    
    private static void remove(String rName, String[] rTags, String rDesc, String rUri, 
            String rChannel, String rOwner, String ezServer, HashDatabase db){
    	//Check strings etc. are valid
    	
    	//Remove resource from database.
}
    
    private static void checkNull(Object value){
    	if(value == null){
    		//do something about error
    	}
    }
}
