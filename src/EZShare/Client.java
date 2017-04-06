package EZShare;

/**
 * Created by Yahang Wu on 2017/3/31.
 * COMP90015 Distributed System Project1 EZServer
 * The client main function
 * include establish the connection with the server
 * and send the command to the server in json string form
 */

import Connection.Connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

    //ip and port
    private static String ip = "1.1.1.1";
    private static int port = 3000;

    public static void main(String[] args){

        Connection connection = new Connection();
        String commandJsonString = connection.clientCli(args);
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
                    String message = input.readUTF();
                    System.out.println(message);
                    if (connection.debugSwitch){
                        System.out.println(Logger.getLogger(Client.class.getName()));
                    }
                }
            }

        }catch (IOException e){
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, e.getMessage());
        }
    }
}
