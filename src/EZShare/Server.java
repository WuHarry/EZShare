package EZShare;

/**
 * Created by Yahang Wu on 2017/3/31.
 * COMP90015 Distributed System Project1 EZServer
 * The server main function
 * include establish the connection with the client
 * and receive the command from the client
 */

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private static int port = 4000;
    private static int counter = 0;
    private static Logger logger = Logger.getLogger(
            Server.class.getName());

    /**
     * The main function of the server
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //load log configuration
        LogConfig.logConfig();

        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        try (ServerSocket server = factory.createServerSocket(port)) {
            logger.info("Starting the EZShare Server");
            logger.fine("Waiting for connection");

            while (true) {
                Socket client = server.accept();
                counter++;

                // Start a new thread for a connection
                Thread t = new Thread(() -> ServerControl.serverClient(client));
                t.start();
            }

        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }


}
