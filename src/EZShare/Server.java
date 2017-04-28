package EZShare;

import Connection.Connection;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Yahang Wu on 2017/3/31.
 * COMP90015 Distributed System Project1 EZServer
 * The server main function
 * include establish the connection with the client
 * and receive the command from the client
 */

public class Server {

    public static int port = 4000;
    private static int counter = 0;
    private static Logger logger = Logger.getLogger(Server.class.getName());

    /**
     * The main function of the server
     * establish the connection to the client
     * handle multiple clients
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //load log configuration
        Common.logConfig();
        //Get the configuration from the Json string
        Connection connection = new Connection();
        connection.serverCli(args);
        //change port
        port = connection.serverPort;

        //store server list
        List<InetSocketAddress> serverList = new ArrayList<InetSocketAddress>();
        List<InetSocketAddress> servers = java.util.Collections.synchronizedList(serverList);

        //Start a thread to exchange server list
        Thread exchange = new Thread(() -> Exchange.serverExchange(connection.exchangeInterval * 10, servers));
        exchange.start();

        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        try (ServerSocket server = factory.createServerSocket(port)) {
            logger.info("Starting the Biubiubiu EZShare Server");
            logger.info("using secret: " + connection.serverSecret);
            logger.info("using advertised hostname: " + Connection.hostName);
            logger.info("bound to port " + port);
            logger.info("started");

            while (true) {
                Socket client = server.accept();
                counter++;

                // Start a new thread for a connection
                Thread t = new Thread(() -> ServerControl.serverClient(client, connection.serverSecret, servers));
                t.start();
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }
}
