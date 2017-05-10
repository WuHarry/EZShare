package EZShare;

import Connection.Connection;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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

    public static int port = 4000;
    private static int counter = 0;
    private static Logger logger = Logger.getLogger(Server.class.getName());
    public static List<InetSocketAddress> servers;
    public static List<InetSocketAddress> secureServers;
    
    // for secure connection 
    public static int sport = 3781;
    public static boolean secureFlag= false;
    
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
//        Boolean secureFlag = false;
        Connection connection = new Connection();
        
        secureFlag = Connection.getSecure();   
        connection.serverCli(args);
        
        //change port
        port = connection.serverPort;
        
        //store server list
        List<InetSocketAddress> serverList = new ArrayList<InetSocketAddress>();
        servers = java.util.Collections.synchronizedList(serverList);
        //Start a thread to exchange server list
        Thread exchange = new Thread(() -> Exchange.serverExchange(connection.exchangeInterval * 1000, servers));
        exchange.start();
        
        //add by Danni Zhao 9 May 2017
        //specify the keystore details (this can be specified as VM arguments as well)
    	//the keystore file contains an application's owncertificate and private key
    	System.setProperty("javax.net.ssl.keyStore","serverKeyStore/serverKeystore.jks");
    	//passowrd to access the private key from the keystore file
    	System.setProperty("javax.net.ssl.keyStorePassword","comp90015");
    	  
    	//enable debugging to view the handshake and communication which happens between the SSLClient
    	System.setProperty("javax.net.debug","all");
    	
        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        SSLServerSocketFactory sslserversocketfactory =(SSLServerSocketFactory) SSLServerSocketFactory
  				.getDefault();
        try (ServerSocket server = factory.createServerSocket(port)) {
            logger.info("Starting the Biubiubiu EZShare Server");
            logger.info("using secret: " + connection.serverSecret);
            logger.info("using advertised hostname: " + Connection.hostName);
            logger.info("bound to port " + port);
            logger.info("started");
            
            SSLServerSocket sslserversocket = (SSLServerSocket) sslserversocketfactory
	  				.createServerSocket(9999);
            
            while (true) {
            	
                Socket client = server.accept();
                SSLSocket sslsocket = (SSLSocket) sslserversocket.accept();
                InputStream inputstream = sslsocket.getInputStream();
    		  	InputStreamReader inputstreamreader =new InputStreamReader(inputstream);
    		  	BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
    		  	System.out.println("Before connection");
    		  	
    		  	//Create buffered reader to read input from the client  
    		  	String string = null;
    		  	
    		  	while((string = bufferedreader.readLine()) != null)
    		  	{
    		      System.out.println(string); // secureClient message
    		      System.out.flush();
    		  	}
                counter++;

                // Start a new thread for a connection
                Thread t = new Thread(() -> ServerControl.serverClient(client, connection.serverSecret, servers));
                t.start();
               
                
            }
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
        }
    }
}
