package EZShare;

import JSON.JSONReader;
import Resource.HashDatabase;
import Resource.Resource;
import exceptions.*;

import com.google.gson.JsonObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
     * @param client  the socket client which is trying to connect to the server
     * @param secret  the server's secret
     * @param servers List of servers the original server currently knows about
     */
    static void serverClient(Socket client, String secret, List<InetSocketAddress> servers) {

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
                                    successResponse(PUBLISH, output);
                                } catch (InvalidResourceException e1) {
                                    invalidResource(PUBLISH, output);
                                } catch (MissingComponentException e2) {
                                    missingResources(PUBLISH, output);
                                } catch (BrokenRuleException e3) {
                                    logger.warning(e3.getLocalizedMessage());
                                    brokenRuleResponse(PUBLISH, output);
                                }
                                break;
                            case REMOVE:
                                //remove
                                try {
                                    Common.checkNull(newResource);
                                    Remove.remove(newResource, db);
                                    successResponse(REMOVE, output);
                                } catch (InvalidResourceException e) {
                                    invalidResource(REMOVE, output);
                                } catch (MissingComponentException e2) {
                                    missingResources(REMOVE, output);
                                } catch (NonExistentResourceException e3) {
                                    logger.warning(e3.getLocalizedMessage());
                                    brokenRuleResponse(REMOVE, output);
                                }
                                break;
                            case SHARE:
                                //share
                                try {
                                    Common.checkNull(newResource);
                                    Share.share(newResource, db, secret);
                                    successResponse(SHARE, output);
                                } catch (InvalidResourceException e1) {
                                    invalidResource(SHARE, output);
                                } catch (MissingComponentException e2) {
                                    JsonObject errorMessage = new JsonObject();
                                    errorMessage.addProperty("response", "error");
                                    errorMessage.addProperty("errorMessage", "missing resource and/or secret");
                                    logger.warning("Share command missing resource or secret.");
                                    output.writeUTF(errorMessage.toString());
                                    logger.fine("[SENT] - " + errorMessage.toString());
                                    output.flush();
                                } catch (IncorrectSecretException e3) {
                                    JsonObject errorMessage = new JsonObject();
                                    errorMessage.addProperty("response", "error");
                                    errorMessage.addProperty("errorMessage", "incorrect secret");
                                    logger.warning("Share command used incorrect secret.");
                                    output.writeUTF(errorMessage.toString());
                                    logger.fine("[SENT] - " + errorMessage.toString());
                                    output.flush();
                                } catch (BrokenRuleException e4) {
                                    logger.warning(e4.getLocalizedMessage());
                                    brokenRuleResponse(SHARE, output);
                                }
                                break;
                            case QUERY:
                                try {
                                    Common.checkNull(newResource);
                                    Set<Resource> resources = Query.query(newResource, db);
                                    if (resources == null) {
                                        output.writeUTF("{\"response\":\"success\"}");
                                        logger.fine("[SENT] - " + "{\"response\":\"success\"}");
                                        output.flush();
                                        output.writeUTF("{\"resultSize\":" + 0 + "}");
                                        logger.fine("[SENT] - " + "{\"resultSize\":" + 0 + "}");
                                        output.flush();
                                    } else {
                                        output.writeUTF("{\"response\":\"success\"}");
                                        logger.fine("[SENT] - " + "{\"response\":\"success\"}");
                                        output.flush();
                                        for (Resource r : resources) {
                                            output.writeUTF(r.toJsonObject().toString());
                                            logger.fine("[SENT] - " + r.toJsonObject().toString());
                                            output.flush();
                                        }
                                        output.writeUTF("{\"resultSize\":" + resources.size() + "}");
                                        logger.fine("[SENT] - " + "{\"resultSize\":" + resources.size() + "}");
                                        output.flush();
                                    }
                                } catch (InvalidResourceException e1) {
                                    invalidResource(QUERY, output);
                                } catch (MissingComponentException e2) {
                                    missingResources(QUERY, output);
                                }
                                break;
                            case FETCH:
                                try {
                                    Common.checkNull(newResource);
                                    Resource resource = Fetch.fetch(newResource, db);
                                    uploadResources(resource, output);
                                } catch (InvalidResourceException e1) {
                                    invalidResource(FETCH, output);
                                } catch (MissingComponentException e2) {
                                    missingResources(FETCH, output);
                                }
                                break;
                            case EXCHANGE:
                                try {
                                    Exchange.exchange(newResource, servers);
                                    successResponse(EXCHANGE, output);
                                } catch (InvalidServerException e1) {
                                    logger.warning("invalid entry in server list");
                                    output.writeUTF("{\"response\":\"error\", \"errorMessage\":\"missing or invalid server list\"}");
                                    logger.fine("[SENT] - {\"response\":\"error\", \"errorMessage\":\"missing or invalid server list\"}");
                                    output.flush();
                                } catch (MissingComponentException e2) {
                                    missingResources(EXCHANGE, output);
                                }
                                break;
                            default:
                                JsonObject errorMessage = new JsonObject();
                                errorMessage.addProperty("response", "error");
                                if (command.isEmpty()) {
                                    logger.warning("missing command");
                                    errorMessage.addProperty("errorMessage", "missing or incorrect type for command");
                                    output.writeUTF(errorMessage.toString());
                                    logger.fine("[SENT] - " + errorMessage.toString());
                                    output.flush();
                                } else {
                                    logger.warning("invalid command");
                                    errorMessage.addProperty("errorMessage", "invalid command");
                                    output.writeUTF(errorMessage.toString());
                                    logger.fine("[SENT] - " + errorMessage.toString());
                                    output.flush();
                                }
                                break;
                        }
                    } else {
                        logger.fine("[RECEIVE] - " + jsonString);
                        output.writeUTF("{\"response\":\"error, invalid string\"}");
                        logger.fine("[SENT] - " + "{\"response\":\"error, invalid string\"}");
                        output.flush();
                    }
//                    System.out.println("Database size: " + db.getDatabaseSize());
                }
            }
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
        }
    }

    /**
     * The method to send client successful response, used by the function
     * PUBLISH, REMOVE, SHARE, EXCHANGE
     *
     * @param command the command that client sent to server
     * @param output  the output stream to the client
     */
    private static void successResponse(String command, DataOutputStream output) {
        try {
            JsonObject successMessage = new JsonObject();
            successMessage.addProperty("response", "success");
            if (command.equals(EXCHANGE)) {
                logger.info("Successfully exchanged server list.");
            } else {
                logger.info("Successfully " + command + " resource.");
            }
            output.writeUTF(successMessage.toString());
            logger.fine("[SENT] - " + successMessage.toString());
            output.flush();
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }

    /**
     * The method to generate response to invalid request
     *
     * @param command the command that server received
     * @param output  the output Stream
     */
    private static void invalidResource(String command, DataOutputStream output) {
        JsonObject errorMessage = new JsonObject();
        errorMessage.addProperty("response", "error");
        if (command.equals(PUBLISH) || command.equals(REMOVE)
                || command.equals(SHARE)) {
            errorMessage.addProperty("errorMessage", "invalid resource");
        } else if (command.equals(QUERY) || command.equals(FETCH)) {
            errorMessage.addProperty("errorMessage", "invalid resourceTemplate");
        }
        try {
            logger.warning("Resource to " + command + " contained incorrect information that could not be recovered from.");
            output.writeUTF(errorMessage.toString());
            logger.fine("[SENT] - " + errorMessage.toString());
            output.flush();
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }

    /**
     * The method to generate response to missing resources request
     *
     * @param command the command that server received
     * @param output  the output Stream
     */
    private static void missingResources(String command, DataOutputStream output) {
        try {
            if (command.equals(PUBLISH) || command.equals(REMOVE)) {
                logger.warning("missing resource");
                output.writeUTF("{\"response\":\"error\", \"errorMessage\":\"missing resource\"}");
                logger.fine("[SENT] - {\"response\":\"error\", \"errorMessage\":\"missing resource\"}");
                output.flush();
            } else if (command.equals(FETCH) || command.equals(QUERY) || command.equals(EXCHANGE)) {
                if (command.equals(EXCHANGE)) {
                    logger.warning("Missing serverList");
                } else {
                    logger.warning("missing resourceTemplate");
                }
                output.writeUTF("{\"response\":\"error\", \"errorMessage\":\"missing resourceTemplate\"}");
                logger.fine("[SENT] - {\"response\":\"error\", \"errorMessage\":\"missing resourceTemplate\"}");
                output.flush();
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }

    /**
     * The method to send client the error message when client break the rule
     *
     * @param command the command that client sent to the server
     * @param output the output stream to the client
     */
    private static void brokenRuleResponse(String command, DataOutputStream output){
        try{
            output.writeUTF("{\"response\":\"error\", \"errorMessage\":\"cannot " + command + " resource\"}");
            logger.fine("[SENT] - {\"response\":\"error\", \"errorMessage\":\"cannot " + command + " resource\"}");
            output.flush();
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }

    /**
     * The method to provide the function to upload files to the client
     *
     * @param resource the resource that client requested
     * @param output   the output stream to the client
     */
    private static void uploadResources(Resource resource, DataOutputStream output) {

        String filePath = resource.getUri().substring(8);
        File f = new File(filePath);
        if (f.exists()) {
            try {
                output.writeUTF("{\"response\":\"success\"}");
                logger.fine("[SENT] - " + "{\"response\":\"success\"}");

                JsonObject trigger = resource.toJsonObject();
                trigger.addProperty("resourceSize", f.length());
                //Sending trigger to the client
                output.writeUTF(trigger.toString());
                logger.fine("[SENT] - " + trigger.toString());

                // Start sending file
                RandomAccessFile byteFile = new RandomAccessFile(f, "r");
                byte[] sendingBuffer = new byte[1024 * 1024];
                int num;
                // While there are still bytes to send..
                while ((num = byteFile.read(sendingBuffer)) > 0) {
                    output.write(Arrays.copyOf(sendingBuffer, num));
                    logger.fine("[SENT] - " + num);
                }
                output.flush();
                output.writeUTF("{\"resultSize\":1}");
                logger.fine("[SENT] - {\"resultSize\":1}");
                output.flush();
                byteFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                output.writeUTF("{\"response\":\"success\"}");
                logger.fine("[SENT] - {\"response\":\"success\"}");
                output.writeUTF("{\"resultSize\":0}");
                logger.fine("[SENT] - {\"resultSize\":0}");
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}