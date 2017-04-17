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
     * @param secret the server's secret
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
            while (true) {
                if (input.available() > 0) {
                    jsonString = input.readUTF();
                    logger.fine("[RECEIVE] - " + jsonString);
                    if (JSONReader.isJSONValid(jsonString)) {
                        //Read command from json.
                        newResource = new JSONReader(jsonString);
                        command = newResource.getCommand();
                        switch (command) {
                            case PUBLISH:
                                //publish
                                try {
                                    Common.checkNull(newResource);
                                    Publish.publish(newResource, db);
                                    JsonObject successMessage = new JsonObject();
                                    successMessage.addProperty("response", "success");
                                    logger.info("Successfully published resource.");
                                    logger.fine("[SENT] - " + successMessage.toString());
                                    output.writeUTF(successMessage.toString());
                                    output.flush();
                                } catch (InvalidResourceException e1) {
                                    JsonObject errorMessage = invalidResponse(PUBLISH);
                                    output.writeUTF(errorMessage.toString());
                                    output.flush();
                                } catch (MissingComponentException e2) {
                                    logger.warning("missing resource");
                                    logger.fine("[SENT] - {\"response\":\"error\", \"errorMessage\":\"missing resource\"}");
                                    output.writeUTF("{\"response\":\"error\", \"errorMessage\":\"missing resource\"}");
                                    output.flush();
                                }
                                break;
                            case REMOVE:
                                //remove
                                try {
                                    Common.checkNull(newResource);
                                    Remove.remove(newResource, db);
                                    JsonObject successMessage = new JsonObject();
                                    successMessage.addProperty("response", "success");
                                    logger.info("Successfully removed resource.");
                                    logger.fine("[SENT] - " + successMessage.toString());
                                    output.writeUTF(successMessage.toString());
                                    output.flush();
                                } catch (InvalidResourceException e) {
                                	JsonObject errorMessage = invalidResponse(REMOVE);
                                    output.writeUTF(errorMessage.toString());
                                    output.flush();
                                }catch (MissingComponentException e2) {
                                    logger.warning("missing resource");
                                    logger.fine("[SENT] - {\"response\":\"error\", \"errorMessage\":\"missing resource\"}");
                                    output.writeUTF("{\"response\":\"error\", \"errorMessage\":\"missing resource\"}");
                                    output.flush();
                                }
                                break;
                            case SHARE:
                                //share
                                try {
                                    Common.checkNull(newResource);
                                    share(newResource, db, secret);
                                } catch (InvalidResourceException e1) {
                                    JsonObject errorMessage = invalidResponse(SHARE);
                                    output.writeUTF(errorMessage.toString());
                                    output.flush();
                                } catch (MissingComponentException e2) {
                                    JsonObject errorMessage = new JsonObject();
                                    errorMessage.addProperty("response", "error");
                                    errorMessage.addProperty("errorMessage", "missing resource and/or secret");
                                    logger.warning("Share command missing resource or secret.");
                                    output.writeUTF(errorMessage.toString());
                                    output.flush();
                                } catch (IncorrectSecretException e3) {
                                    JsonObject errorMessage = new JsonObject();
                                    errorMessage.addProperty("response", "error");
                                    errorMessage.addProperty("errorMessage", "incorrect secret");
                                    logger.warning("Share command used incorrect secret.");
                                    output.writeUTF(errorMessage.toString());
                                    output.flush();
                                }
                                break;
                            case QUERY:
                                try {
                                    Common.checkNull(newResource);
                                    Query.query(newResource, db);
                                } catch (InvalidResourceException e1) {
                                    JsonObject errorMessage = new JsonObject();
                                    errorMessage.addProperty("response", "error");
                                    errorMessage.addProperty("errorMessage", "invalid resourceTemplate");
                                    logger.warning("Resource to query contained incorrect information that could not be recovered from.");
                                    output.writeUTF(errorMessage.toString());
                                    output.flush();
                                } catch (MissingComponentException e2) {
                                    logger.warning("missing resourceTemplate");
                                    logger.fine("[SENT] - {\"response\":\"error\", \"errorMessage\":\"missing resourceTemplate\"}");
                                    output.writeUTF("{\"response\":\"error\", \"errorMessage\":\"missing resourceTemplate\"}");
                                    output.flush();
                                }
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
                    } else {
                        logger.fine("[RECEIVE] - " + jsonString);
                        logger.fine("[SENT] - " + "{\"response\":\"error, invalid string\"}");
                        output.writeUTF("{\"response\":\"error, invalid string\"}");
                        output.flush();
                    }
                    //For test for now
                    System.out.println("Database size: " + db.getDatabaseSize());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    /**
     * The method to generate response to invalid request
     *
     * @param command the command that server received
     * @return the error message json object
     */
    private static JsonObject invalidResponse(String command){
        JsonObject errorMessage = new JsonObject();
        errorMessage.addProperty("response", "error");
        errorMessage.addProperty("errorMessage", "invalid resource");
        logger.warning("Resource to " + command + " contained incorrect information that could not be recovered from.");
        logger.fine("[SENT] - " + errorMessage.toString());
        return errorMessage;
    }

    /**
     * Validates then shares a resource with a file uri, inserting it into the database.
     *
     * @param resource The resource to be shared.
     * @param db       Database to insert the resource into.
     * @throws InvalidResourceException  If the resource contains incorrect information or invalid fields, this is thrown.
     * @throws IncorrectSecretException  If the secret supplied does not match server secret, this is thrown.
     * @throws MissingComponentException If secret is missing from command, this is thrown.
     */
    private static void share(JSONReader resource, HashDatabase db, String serverSecret) throws InvalidResourceException, IncorrectSecretException, MissingComponentException {
        String name = resource.getResourceName();
        String description = resource.getResourceDescription();
        String channel = resource.getResourceChannel();
        String owner = resource.getResourceOwner();
        String uri = resource.getResourceUri();
        String[] tags = resource.getResourceTags();
        String ezserver = resource.getResourceEZserver();
        String secret = resource.getSecret();

        if (secret == null) {
            //missing secret
            throw new MissingComponentException();
        }

        //Check secret
        if (!secret.equals(serverSecret)) {
            //Incorrect secret, error.
            throw new IncorrectSecretException();
        }

        //Validate strings
        if (!Common.validateResource(name, description, tags, uri, channel, owner)) {
            throw new InvalidResourceException("Trying to share Resource with illegal fields.");
        }
        //Validate uri
        try {
            URI path = new URI(uri);
            if (!path.isAbsolute() || !path.getScheme().equals("file")) {
                throw new InvalidResourceException("Trying to share resource with non-absolute or non-file uri.");
            }
            File f = new File(path);
            if (!f.exists() || f.isDirectory()) {
                throw new InvalidResourceException("File referenced by uri does not exist.");
            }
        } catch (URISyntaxException e) {
            throw new InvalidResourceException("Attempting to share resource with invalid uri syntax.");
        }

        //Remove if match pKey in db
        Resource match = db.pKeyLookup(channel, uri);
        if (match != null) {
            db.deleteResource(match);
        }

        //Add to db
        db.insertResource(new Resource(name, description, Arrays.asList(tags),
                uri, channel, owner, ezserver));
    }

}
