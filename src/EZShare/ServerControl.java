package EZShare;

import JSON.JSONReader;
import Resource.HashDatabase;
import exceptions.IncorrectSecretException;
import exceptions.InvalidResourceException;
import exceptions.MissingComponentException;

import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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
                                    Share.share(newResource, db, secret);
                                } catch (InvalidResourceException e1) {
                                    JsonObject errorMessage = invalidResponse(SHARE);
                                    output.writeUTF(errorMessage.toString());
                                    output.flush();
                                } catch (MissingComponentException e2) {
                                    JsonObject errorMessage = new JsonObject();
                                    errorMessage.addProperty("response", "error");
                                    errorMessage.addProperty("errorMessage", "missing resource and/or secret");
                                    logger.warning("Share command missing resource or secret.");
                                    logger.fine("[SENT] - " + errorMessage.toString());
                                    output.writeUTF(errorMessage.toString());
                                    output.flush();
                                } catch (IncorrectSecretException e3) {
                                    JsonObject errorMessage = new JsonObject();
                                    errorMessage.addProperty("response", "error");
                                    errorMessage.addProperty("errorMessage", "incorrect secret");
                                    logger.warning("Share command used incorrect secret.");
                                    logger.fine("[SENT] - " + errorMessage.toString());
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
//                    System.out.println("Database size: " + db.getDatabaseSize());
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
}
