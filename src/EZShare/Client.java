package EZShare;

/**
 * Created by Yahang Wu on 2017/3/31.
 * COMP90015 Distributed System Project1 EZServer
 * The client main function
 * include establish the connection with the server
 * and send the command to the server in json string form
 */

import Connection.Connection;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

    //ip and port
    private static String ip = "1.1.1.1";
    private static int port = 3000;
    //Mark whether next response from server has some file
    private static boolean hasResources = false;

    public static void main(String[] args){

        Connection connection = new Connection();
        String commandJsonString = connection.clientCli(args);

        System.out.println(commandJsonString);
        //update ip and port
        ip = connection.host;
        port = connection.port;

        //new client socket
        try(Socket socket = new Socket(ip, port)){
            //input stream
            DataInputStream input = new DataInputStream(socket.getInputStream());
            //output stream
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            if(commandJsonString != null){
                output.writeUTF(commandJsonString);
                output.flush();
            }else {
                System.out.println("Command error");
            }

            while(true){
                if(input.available() > 0){
                    if(hasResources){
                        try{
                            FileOutputStream fileOutputStream = new
                                    FileOutputStream("1.jpg");
                            byte[] buffer = new byte[1024];
                            int len = 0;
                            while ((len = input.read(buffer)) != -1) {
                                fileOutputStream.write(buffer, 0, len);
                            }
                            fileOutputStream.close();
                            hasResources = false;
                            System.out.println("Resource read successful.");
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    } else {
                        String message = input.readUTF();
                        System.out.println(message);
                        checkResources(message);
                    }
                }
            }

        }catch (IOException e){
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, e.getMessage());
        }
    }

    /**
     * The method to check if there is a resource need to be stored to file.
     * @param message the server returned message
     */
    public static void checkResources (String message){
        JsonParser parser = new JsonParser();
        JsonObject response = (JsonObject) parser.parse(message);
        if(response.has("resourceSize")){
            hasResources = true;
        }
    }
}
