package EZShare;

import Connection.Connection;
import JSON.JSONReader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.*;

/**
 * Created by Yahang Wu on 2017/3/31.
 * COMP90015 Distributed System Project1 EZServer
 * The client main function
 * include establish the connection with the server
 * and send the command to the server in json string form
 */

public class Client {

    //ip and port
    private static String ip = "1.1.1.1";
    private static int port = 4000;
    //Mark whether next response from server has some file
    private static boolean hasResources = false;
    //Record the resource size if the resource is a file
    private static long resourceSize = 0;
    private static String resourceName = "";
    //Mark the end of the connection
    private static boolean theEnd = false;
    //Logger
    private static final Logger logger = Logger.getLogger(
            Client.class.getName());

    /**
     * The Client main method
     * Establish the connection to the server
     * send the command json String to the server
     * get the response from the server
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //load log configuration
        Common.logConfig();
        //get the command json string
        Connection connection = new Connection();
        String commandJsonString = connection.clientCli(args);
        //update ip
        ip = connection.host;

        //new client socket
        try {
            Socket socket;
            if (Connection.secureConnection) {
                port = connection.port;
                socket = Common.initClientSSL().createSocket(ip, port);
            } else {
                port = connection.port;
                socket = new Socket(ip, port);
            }

            //update the id for subscribe if no id is read from command line
            if (Connection.command.equals("SUBSCRIBE")) {
                connection.id = ("" + socket.getLocalSocketAddress()).substring(1);
                commandJsonString = connection.clientCli(args);
            }

            //input stream
            DataInputStream input =
                    new DataInputStream(socket.getInputStream());
            //output stream
            DataOutputStream output =
                    new DataOutputStream(socket.getOutputStream());
            if (commandJsonString != null) {
                output.writeUTF(commandJsonString);
                System.out.println(commandJsonString);
                if (Connection.debugSwitch) {
                    logger.info("Debug mode on");
                    logger.fine("[SENT] - " + commandJsonString);
                }
                output.flush();
            }
            if (Connection.command.equals("SUBSCRIBE")) {
                //listen to the client's console input
                Thread listener = new Thread(() -> unsubscribe(connection.id, output));
                listener.start();
            }
            //to check the input in a short period
            socket.setSoTimeout(20);
            while (true) {
                try {
                    if (hasResources) {
                        downloadResources(input);
                    } else {
                        String message = input.readUTF();
                        //check if debug mode is on
                        if (Connection.debugSwitch) {
                            logger.fine("[RECEIVE] - " + message);
                        }
                        System.out.println(message);
                        checkResources(message, Connection.command);
                        //check whether it is time to close connection
                        if (theEnd) break;
                    }
                } catch (SocketTimeoutException e) {
                    //just aimed to check weather the input is empty
                }
            }
            socket.close();
        } catch (IOException e) {
            logger.warning("[ERROR] - Can not establish connection.");
        }
    }

    /**
     * The method to check if there is a resource need to be stored to file.
     * Also check whether it is the end of the connection.
     *
     * @param message the server returned message
     * @param command the client command sent to the server
     */
    private static void checkResources(String message, String command) {
        if (JSONReader.isJSONValid(message)) {
            JsonParser parser = new JsonParser();
            JsonObject response = (JsonObject) parser.parse(message);
            if (response.has("resourceSize")) {
                hasResources = true;
                if (Connection.debugSwitch) {
                    logger.info("{\"resourceSize\":" +
                            response.get("resourceSize") + "}");
                }
                System.out.println("exact bytes of resource");
                resourceSize = response.get("resourceSize").getAsLong();
                String[] uri = response.get("uri").getAsString().split("/");
                resourceName = uri[uri.length - 1];
            } else if (response.has("resultSize")) {
                theEnd = true;
            } else if (response.has("response")) {
                if (response.get("response").getAsString().equals("error")) {
                    theEnd = true;
                }
                if ((command.equals("PUBLISH") || command.equals("REMOVE") ||
                        command.equals("SHARE") || command.equals("EXCHANGE"))
                        && response.get("response").getAsString().equals("success")) {
                    theEnd = true;
                }
            }
        }
    }

    /**
     * The method to download the resources
     *
     * @param input the DataInputStream
     */
    private static void downloadResources(DataInputStream input) {
        try {
            //reset trigger
            hasResources = false;
            //get the separator from the system, different between linux and win
            String separator = File.separator;
            //get the system path
            String path = System.getProperty("user.dir");
            path += (separator + "client_file" + separator);
            //make the dir if not exist
            File file = new File(path);
            if (!file.exists()) file.mkdir();
            //get file dir
            String fileName = path + separator + resourceName;
            RandomAccessFile downloadingFile =
                    new RandomAccessFile(fileName, "rw");
            long fileSizeRemaining = resourceSize;
            int chunkSize = setChunkSize(fileSizeRemaining);
            byte[] receiveBuffer = new byte[chunkSize];
            System.out.println("file " + resourceName + " is of size " + fileSizeRemaining);
            int num;
            while ((num = input.read(receiveBuffer)) > 0) {
                //write received bytes into the RandomAccessFile
                downloadingFile.write(Arrays.copyOf(receiveBuffer, num));
                //Reduce the fileSizeRemaining
                fileSizeRemaining -= num;
                chunkSize = setChunkSize(fileSizeRemaining);
                //update buffer
                receiveBuffer = new byte[chunkSize];
                //when file size is zero, break
                if (fileSizeRemaining == 0) {
                    break;
                }
            }
            if (Connection.debugSwitch) {
                logger.info("Resource read successfully.");
            }
            System.out.println("Resource read successfully.");
            //reset resource size and name
            resourceSize = 0;
            resourceName = "";
            downloadingFile.close();
        } catch (IOException e) {
            logger.warning("[ERROR] - Resources download failed!");
            e.printStackTrace();
        }
    }

    /**
     * The unsubscribe method for the client, used to send the message to the
     * server
     *
     * @param id     the client id
     * @param output the output DataOutputStream
     */
    private static void unsubscribe(String id, DataOutputStream output) {
        if (readFromConsole()) {
            try {
                String unsubscribeMessage = Connection.generateUnsubscribeMessage(id);
                if (unsubscribeMessage != null) {
                    output.writeUTF(unsubscribeMessage);
                    if (Connection.debugSwitch) {
                        logger.fine("[SENT] - " + unsubscribeMessage);
                    }
                    output.flush();
                }
            } catch (IOException e) {
                logger.warning("Cannot close socket connection.");
            }
        }
    }

    /**
     * The method to listen to the user's input in the console
     *
     * @return whether user input a "Enter"
     */
    private static boolean readFromConsole() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String next = scanner.nextLine();
            if (next.equals("\r") || next.equals("\n") || next.equals("")) {
                scanner.close();
                return true;
            }
        }
    }

    /**
     * @param chunkSize the chunk size you want to set to
     * @return the int form chunk size
     */
    private static int setChunkSize(long chunkSize) {
        return (int) chunkSize;
    }
}