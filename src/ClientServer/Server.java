package ClientServer;

import javax.net.ServerSocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by harry on 2017/3/31.
 */

public class Server {

    private static int port = 3000;
    private static int counter = 0;

    public static void main(String[] args) {

        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        try (ServerSocket server = factory.createServerSocket(port)) {
            System.out.println("waiting for connection ......");

            while (true) {
                Socket client = server.accept();
                counter++;

                // Start a new thread for a connection
                Thread t = new Thread(() -> serverClient(client));
                t.start();
            }

        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    private static void serverClient(Socket client) {
        try (Socket clientSocket = client) {
            //input stream
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            //output stream
            DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

            System.out.println("ClientServer: " + input.readUTF());

            output.writeUTF("Hi client " + counter);
            output.flush();

        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

}
