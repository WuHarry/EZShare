package EZShare;

/**
 * Created by Yahang Wu on 2017/3/31.
 * COMP90015 Distributed System Project1 EZServer
 * The client main function
 * include establish the connection with the server
 * and send the command to the server in json string form
 */

import Connection.Connection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.util.logging.*;

public class Client {

    //ip and port
    private static String ip = "1.1.1.1";
    private static int port = 3000;
    //Mark whether next response from server has some file
    private static boolean hasResources = false;
    //Logger
    private static final Logger logger = Logger.getLogger(
            Client.class.getName());

    public static void main(String[] args) {

        //Record the resource size if the resource is a file
        int resourceSize = 0;
        //Load log config file
        try {
            FileInputStream config = new FileInputStream("logger.properties");
            LogManager.getLogManager().readConfiguration(config);

        } catch (IOException e) {
            System.out.println("WARNING: Can not load log configuration file");
            System.out.println("WARNING: Logging not configured");
            e.printStackTrace();
        }

        //get the command json string
        Connection connection = new Connection();
        String commandJsonString = connection.clientCli(args);

        //update ip and port
        ip = connection.host;
        port = connection.port;

        //new client socket
        try (Socket socket = new Socket(ip, port)) {
            //input stream
            DataInputStream input =
                    new DataInputStream(socket.getInputStream());
            //output stream
            DataOutputStream output =
                    new DataOutputStream(socket.getOutputStream());
            if (Connection.debugSwitch) {
                logger.info("Debug mode on");
                logger.fine("[SENT] - " + commandJsonString);
            }
            if (commandJsonString != null) {
                output.writeUTF(commandJsonString);
                output.flush();
            }

            while (true) {
                if (input.available() > 0) {
                    if (hasResources) {
                        downloadResources(input,resourceSize);
                    } else {
                        String message = input.readUTF();
                        //check if debug mode is on
                        if (Connection.debugSwitch) {
                            logger.fine("[RECEIVE] - " + message);
                        }
                        System.out.println(message);
                        resourceSize = checkResources(message);
                    }
                }
            }
        } catch (IOException e) {
            logger.warning("[ERROR] - Unexpected response");
        }
    }

    /**
     * The method to check if there is a resource need to be stored to file.
     *
     * @param message the server returned message
     * @return the resource size if there is a resource file otherwise return 0
     */
    private static int checkResources(String message) {
        if (isJSONValid(message)) {
            JsonParser parser = new JsonParser();
            JsonObject response = (JsonObject) parser.parse(message);
            if (response.has("resourceSize")) {
                hasResources = true;
                if (Connection.debugSwitch) {
                    logger.info("{\"resourceSize\":" +
                            response.get("resourceSize") + "}");
                }
                System.out.println("exact bytes of resource");
                return response.get("resourceSize").getAsInt();
            } else {
                hasResources = false;
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * The method to check whether a string is a valid json string
     *
     * @param jsonInString
     * @return true for the string is a json string
     */
    private static boolean isJSONValid(String jsonInString) {
        Gson gson = new Gson();
        try {
            gson.fromJson(jsonInString, Object.class);
            return true;
        } catch (com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }

    /**
     * The method to download the resources
     * @param input the DataInputStream
     * @param resourceSize the size of the resource which need to be download
     */
    private static void downloadResources(DataInputStream input,int resourceSize){
        try {
            hasResources = false;
            FileOutputStream fileOutputStream = new
                    FileOutputStream("1.jpg");
            byte[] buffer = new byte[1024];
            int bytesLeft = resourceSize;
            while (bytesLeft > 0) {
                int read = input.read(buffer, 0, Math.min(bytesLeft, buffer.length));
                if (read == -1) {
                    throw new EOFException("Unexpected end of data");
                }
                fileOutputStream.write(buffer, 0, read);
                bytesLeft -= read;
            }
            fileOutputStream.close();
            if (Connection.debugSwitch) {
                logger.info("Resource read successfully.");
            }
            System.out.println("Resource read successfully.");
        } catch (IOException e) {
            logger.warning("[ERROR] - Resources download failed!");
            e.printStackTrace();
        }
    }
}