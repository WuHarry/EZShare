package EZShare;

import JSON.JSONReader;
import Resource.HashDatabase;
import Resource.Resource;
import exceptions.IncorrectSecretException;
import exceptions.InvalidResourceException;
import exceptions.MissingComponentException;

import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
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
     *
     * @param client the socket client which is trying to connect to the server
     */
    static void serverClient(Socket client, String secret) {
        try (Socket clientSocket = client) {
            JSONReader newResource;
            String command;
            //input stream
            DataInputStream input =
                    new DataInputStream(clientSocket.getInputStream());
            //output stream
            DataOutputStream output =
                    new DataOutputStream(clientSocket.getOutputStream());

            String jsonString;
            if (input.available() != 0) {
                jsonString = input.readUTF();
                if (JSONReader.isJSONValid(jsonString)) {
                    //Read command from json.
                    newResource = new JSONReader(jsonString);
                    command = newResource.getCommand();
                    switch (command) {
                        case PUBLISH:
                            //publish
                            try{
                            	checkNull(newResource);
                            	publish(newResource, db);
                            	JsonObject successMessage = new JsonObject();
                                successMessage.addProperty("response", "success");
                                logger.fine("Successfully published resource.");
                                output.writeUTF(successMessage.toString());
                                output.flush();
                            }catch(InvalidResourceException e1){
                            	JsonObject errorMessage = new JsonObject();
                            	errorMessage.addProperty("response", "error");
                            	errorMessage.addProperty("errorMessage", "invalid resource");
                            	logger.warning("Resource to publish contained incorrect information that could not be recovered from.");
                            	output.writeUTF(errorMessage.toString());
                            	output.flush();
                            }catch(MissingComponentException e2){
                            	logger.warning("missing resource");
                                try {
                                    logger.fine("[SENT] - {\"response\":\"error\", \"errorMessage\":\"missing resource\"}");
                                    output.writeUTF("{\"response\":\"error\", \"errorMessage\":\"missing resource\"}");
                                    output.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        case REMOVE:
                            //remove
						try {
							checkNull(newResource);
							remove(newResource, db);
						} catch (MissingComponentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                            break;
                        case SHARE:
                            //share
                        	try{
                        		checkNull(newResource);
                        		share(newResource, db, secret);
                        	}catch(InvalidResourceException e1){
                        		JsonObject errorMessage = new JsonObject();
                            	errorMessage.addProperty("response", "error");
                            	errorMessage.addProperty("errorMessage", "invalid resource");
                            	logger.warning("Resource to share contained incorrect information that could not be recovered from.");
                            	output.writeUTF(errorMessage.toString());
                            	output.flush();
                        	}catch(MissingComponentException e2){
                        		JsonObject errorMessage = new JsonObject();
                        		errorMessage.addProperty("response", "error");
                        		errorMessage.addProperty("errorMessage", "missing resource and/or secret");
                        		logger.warning("Share command missing resource or secret.");
                        		output.writeUTF(errorMessage.toString());
                        		output.flush();
                        	}catch(IncorrectSecretException e3){
                        		JsonObject errorMessage = new JsonObject();
                        		errorMessage.addProperty("response", "error");
                        		errorMessage.addProperty("errorMessage", "incorrect secret");
                        		logger.warning("Share command used incorrect secret.");
                        		output.writeUTF(errorMessage.toString());
                        		output.flush();
                        	}
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
                            if (command.isEmpty()) {
                                logger.warning("missing command");
                                errorMessage.addProperty("errorMessage", "missing or incorrect type for command");
                                output.writeUTF(errorMessage.toString());
                                output.flush();
                            } else {
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
                } else {
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

    //Probably need a method to check strings etc. are valid, haven't think clearly yet...
    //or should we use a method to check if it is legal in this class and pass it to the new class to do the six functions?
    /**
     * Validates and inserts a resource into the database for future sharing, which is not a file.
     * @param resource The resource to be published.
     * @param db The database the resource should be inserted into.
     * @throws InvalidResourceException If the resource supplied contains illegal fields, this is thrown.
     */
    private static void publish(JSONReader resource, HashDatabase db) throws InvalidResourceException {
        String name = resource.getResourceName();
        String description = resource.getResourceDescription();
        String channel = resource.getResourceChannel();
        String owner = resource.getResourceOwner();
        String uri = resource.getResourceUri();
        String[] tags = resource.getResourceTags();
        String ezserver = resource.getResourceEZserver();
        
        //Check strings etc. are valid
        if(!validateResource(name, description, tags, uri, channel, owner, ezserver)){
        	throw new InvalidResourceException("Trying to publish Resource with illegal fields.");
        }
        try{
    		URI path = new URI(uri);
    		if(!path.isAbsolute() || path.getScheme().equals("file")){
    			throw new InvalidResourceException("Trying to publish resource with non-absolute or file uri.");
    		}
    	}catch(URISyntaxException e){
    		throw new InvalidResourceException("Attempting to publish resource with invalid uri syntax.");
    	}
        
        //Make sure matching primary key resources are removed.
        Resource match = db.pKeyLookup(channel, uri);
        if(match != null){
        	db.deleteResource(match);
        }
        
        //Add resource to database.
        db.insertResource(new Resource(name, description, Arrays.asList(tags), 
        		          uri, channel, owner, ezserver));
    }

    private static void remove(JSONReader resource, HashDatabase db) {
        //Check strings etc. are valid

        //Remove resource from database.
    }
    
    /**
     * Validates then shares a resource with a file uri, inserting it into the database.
     * @param resource The resource to be shared.
     * @param db Database to insert the resource into.
     * @throws InvalidResourceException If the resource contains incorrect information or invalid fields, this is thrown.
     * @throws IncorrectSecretException If the secret supplied does not match server secret, this is thrown.
     * @throws MissingSecretException  If secret is missing from command, this is thrown.
     */
    private static void share(JSONReader resource, HashDatabase db, String serverSecret) throws InvalidResourceException, IncorrectSecretException, MissingComponentException{
    	String name = resource.getResourceName();
        String description = resource.getResourceDescription();
        String channel = resource.getResourceChannel();
        String owner = resource.getResourceOwner();
        String uri = resource.getResourceUri();
        String[] tags = resource.getResourceTags();
        String ezserver = resource.getResourceEZserver();    	
    	String secret = resource.getSecret();
        
    	if(secret == null){
    		//missing secret
    		throw new MissingComponentException();
    	}
    	
    	//Check secret
        if(!secret.equals(serverSecret)){
        	//Incorrect secret, error.
        	throw new IncorrectSecretException();
        }
        
    	//Validate strings
        if(!validateResource(name, description, tags, uri, channel, owner, ezserver)){
        	throw new InvalidResourceException("Trying to share Resource with illegal fields.");
        }
    	//Validate uri
    	try{
    		URI path = new URI(uri);
    		if(!path.isAbsolute() || !path.getScheme().equals("file")){
    			throw new InvalidResourceException("Trying to share resource with non-absolute or non-file uri.");
    		}
    		File f = new File(path);
    		if(!f.exists() || f.isDirectory()){
    			throw new InvalidResourceException("File referenced by uri does not exist.");
    		}
    	}catch(URISyntaxException e){
    		throw new InvalidResourceException("Attempting to share resource with invalid uri syntax.");
    	}
        
    	//Remove if match pKey in db
        Resource match = db.pKeyLookup(channel, uri);
        if(match != null){
        	db.deleteResource(match);
        }
        
    	//Add to db
        db.insertResource(new Resource(name, description, Arrays.asList(tags), 
		          uri, channel, owner, ezserver));
    }


    /**
     * Returns true only if the String s is valid according to rules for resource field 
     * strings supplied to the server.
     * @param s String to be checked.
     * @return True if s is valid, false otherwise.
     */
    private static boolean validateString(String s){
    	return !(s.contains("\0") || s.charAt(0) == ' ' || s.charAt(s.length() - 1) == ' ');
    }
    
    /**
     * Returns true only if the described resource is made up of valid components (in terms of 
     * String composition, not particular logic of a command).
     * @param name 
     * @param desc 
     * @param tags
     * @param uri
     * @param channel
     * @param owner
     * @param ezServer
     * @return
     */
    private static boolean validateResource(String name, String desc, String[] tags, String uri, 
    		                                String channel, String owner, String ezServer){
    	if(!(validateString(name) && validateString(desc) && validateString(channel) &&
             validateString(owner) && validateString(uri) && validateString(ezServer))){
           	//Error with resource
           	return false;
        }
        for(String tag: tags){
           	if(!validateString(tag)){
           		return false;
           	}
        }
        if(owner.equals("*")){
           	return false;
        }
    	return true;
    }
    
    /**
     * Throws exception if curr does not contain fields necessary to describe a resource.
     * @param curr The JSONReader which will be checked for a complete resource.
     * @param output Output to write to.
     * @throws MissingComponentException Thrown if curr does not contain full resource descriptor.
     */
    private static void checkNull(JSONReader curr) throws MissingComponentException {
        if (curr.getResourceName() == null || curr.getResourceChannel() == null || curr.getResourceUri() == null ||
                curr.getResourceDescription() == null || curr.getResourceOwner() == null || curr.getResourceTags() == null) {
            throw new MissingComponentException("Missing resource.");
        }
        // to be continued
    }

}
