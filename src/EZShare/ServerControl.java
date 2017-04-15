package EZShare;

import JSON.JSONReader;
import Resource.HashDatabase;
import com.google.gson.JsonObject;

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
        	JSONReader newResource;
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
                    newResource = new JSONReader(jsonString);
                	command = newResource.getCommand();
					switch (command) {
						case PUBLISH:
							//publish
                            checkNull(newResource, output);
							publish(newResource, db);
							break;
						case REMOVE:
							//remove
                            checkNull(newResource, output);
							remove(newResource, db);
							break;
						case SHARE:
							//share
							break;
						case QUERY:
							//query
							break;
						case FETCH:
							//fetch
							break;
						case EXCHANGE:
							//exchange
							break;
						default:
                            JsonObject errorMessage = new JsonObject();
                            errorMessage.addProperty("response", "error");
						    if (command.isEmpty()){
                                logger.warning("missing command");
                                errorMessage.addProperty("errorMessage", "missing or incorrect type for command");
                                output.writeUTF(errorMessage.toString());
                                output.flush();
                            }else {
						        logger.warning("invalid command");
						        errorMessage.addProperty("errorMessage", "invalid command");
						        output.writeUTF(errorMessage.toString());
						        output.flush();
                            }
							break;
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
    
    private static void publish(JSONReader resource, HashDatabase db){
    	//Check strings etc. are valid
    	
    	//Make sure matching primary key resources are removed.
    	
    	//Add resource to database.
    }
    
    private static void remove(JSONReader resource, HashDatabase db){
    	//Check strings etc. are valid
    	
    	//Remove resource from database.
}
    
    private static void checkNull(JSONReader curr, DataOutputStream output){
    	if(curr.getResourceName() == null || curr.getResourceChannel() == null || curr.getResourceUri() == null ||
                curr.getResourceDescription() == null || curr.getResourceOwner() == null || curr.getResourceTags() == null){
    	    logger.warning("missing resource");
    	    try{
                output.writeUTF("{\"response\":\"error\", \"errorMessage\":\"missing resource\"}");
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
    	}
    	// to be continued

    }
}
