package EZShare;

import Connection.Connection;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Yahang Wu on 2017/3/31.
 * COMP90015 Distributed System Project1 EZServer
 * The server main function
 * include establish the connection with the client
 * and receive the command from the client
 */

public class Server {

    public static int port = 3781;
    private static int counter = 0;
    private static Logger logger = Logger.getLogger(Server.class.getName());
    public static List<InetSocketAddress> servers;

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
        servers = java.util.Collections.synchronizedList(serverList);
        //Start a thread to exchange server list
        Thread exchange = new Thread(() -> Exchange.serverExchange(connection.exchangeInterval * 1000, servers));
        exchange.start();

//        ServerSocketFactory factory = ServerSocketFactory.getDefault();
//        try (ServerSocket server = factory.createServerSocket(port)) {
        try{
            SSLServerSocket server = (SSLServerSocket) initSSL().createServerSocket(3781);
            logger.info("Starting the Biubiubiu EZShare Server");
            logger.info("using secret: " + connection.serverSecret);
            logger.info("using advertised hostname: " + Connection.hostName);
            logger.info("bound to port " + port);
            logger.info("started");

            while (true) {
                Socket client = server.accept();
                //Socket client = server.accept();
                counter++;

                // Start a new thread for a connection
                Thread t = new Thread(() -> ServerControl.serverClient(client, connection.serverSecret, servers));
                t.start();
            }
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
        }
    }

    /**
     * The method to initial SSL socket for client
     * include reading the certifications and generate ssl socketFactory
     *
     * @return the initialed SSLSocketFactory
     */
    private static SSLServerSocketFactory initSSL(){
        try{
            SSLContext context = SSLContext.getInstance("SSL");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            KeyStore keyStore = KeyStore.getInstance("JKS");

            String password = "comp90015";
            InputStream inputStream = Server.class.getResourceAsStream("/certifications/serverKeystore.jks");

            keyStore.load(inputStream,password.toCharArray());

            keyManagerFactory.init(keyStore, password.toCharArray());
            context.init(keyManagerFactory.getKeyManagers(),null,null);

            return context.getServerSocketFactory();

        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException | KeyManagementException | UnrecoverableKeyException e) {
            logger.warning("Cannot load certifications for ssl connection.");
            logger.warning("initial failed!");
        }
        return (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    }
}
