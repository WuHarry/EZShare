package EZShare;

import Connection.Connection;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
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

    private static Logger logger = Logger.getLogger(Server.class.getName());

    public static int port = 4000;
    private static int securePort = 3781;
    private static String serverSecret = "";
    static List<InetSocketAddress> servers;
    static List<InetSocketAddress> secureServers;

    private static final boolean IS_SECURE = true;
    private static final boolean NOT_SECURE = false;

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
        //change port and secret
        port = connection.serverPort;
        securePort = connection.securePort;
        serverSecret = connection.serverSecret;

        //store server list
        List<InetSocketAddress> serverList = new ArrayList<>();
        servers = java.util.Collections.synchronizedList(serverList);
        secureServers = java.util.Collections.synchronizedList(serverList);
        //Start a thread to exchange server list
        Thread exchange = new Thread(() -> Exchange.serverExchange(connection.exchangeInterval * 1000, servers, NOT_SECURE));
        exchange.start();
        Thread secureExchange = new Thread(() -> Exchange.serverExchange(connection.exchangeInterval * 1000, secureServers, IS_SECURE));
        secureExchange.start();

        NormalSocket normal = new NormalSocket();
        SecureSocket secure = new SecureSocket();
        Thread normalThread = new Thread(normal);
        Thread secureThread = new Thread(secure);
        normalThread.start();
        secureThread.start();
    }

    static class NormalSocket implements Runnable {

        public final boolean isSecure = false;
        @Override
        public void run() {
            ServerSocketFactory factory = ServerSocketFactory.getDefault();
            try (ServerSocket server = factory.createServerSocket(port)) {
                logger.info("Starting the Biubiubiu EZShare Server");
                logger.info("using secret: " + serverSecret);
                logger.info("using advertised hostname: " + Connection.hostName);
                logger.info("bound to port " + port);
                logger.info("started");

                while (true) {
                    Socket client = server.accept();

                    // Start a new thread for a connection
                    Thread t = new Thread(() -> ServerControl.serverClient(client, serverSecret, servers, isSecure));
                    t.start();
                }
            } catch (IOException ex) {
                logger.warning(ex.getMessage());
            }
        }
    }

    static class SecureSocket implements Runnable {

        public final boolean isSecure = true;
        @Override
        public void run() {
            try (SSLServerSocket server = (SSLServerSocket) initSSL().createServerSocket(securePort)) {
                logger.info("Starting the Biubiubiu EZShare Secure Server");
                logger.info("using secret: " + serverSecret);
                logger.info("using advertised hostname: " + Connection.hostName);
                logger.info("bound to secure port " + securePort);
                logger.info("started");

                while (true) {
                    Socket client = server.accept();

                    // Start a new thread for a connection
                    Thread t = new Thread(() -> ServerControl.serverClient(client, serverSecret, secureServers, isSecure));
                    t.start();
                }
            } catch (IOException ex) {
                logger.warning(ex.getMessage());
            }
        }
    }

    /**
     * The method to initial SSL socket for client
     * include reading the certifications and generate ssl socketFactory
     *
     * @return the initialed SSLSocketFactory
     */
    private static SSLServerSocketFactory initSSL() {
        try {
            SSLContext context = SSLContext.getInstance("SSL");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            KeyStore keyStore = KeyStore.getInstance("JKS");

            String password = "comp90015";
            InputStream inputStream = Server.class.getResourceAsStream("/certifications/serverKeystore.jks");

            keyStore.load(inputStream, password.toCharArray());

            keyManagerFactory.init(keyStore, password.toCharArray());
            context.init(keyManagerFactory.getKeyManagers(), null, null);

            return context.getServerSocketFactory();

        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException | KeyManagementException | UnrecoverableKeyException e) {
            logger.warning("Cannot load certifications for ssl connection.");
            logger.warning("initial failed!");
        }
        return (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    }
}
