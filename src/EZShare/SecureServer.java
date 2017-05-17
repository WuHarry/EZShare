package EZShare;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import Connection.Connection;

public class SecureServer {

    public static int port = 4000;
    private static int counter = 0;
    private static Logger logger = Logger.getLogger(Server.class.getName());
    public static List<InetSocketAddress> servers;
    public static List<InetSocketAddress> secureServers;
    
    // for secure connection 
    public static int sport = 3781;
    
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
        Boolean secureFlag = false;
        Connection connection = new Connection();
        
        secureFlag = Connection.getSecure();   
        connection.serverCli(args);
 
        //change port
        port = connection.serverPort;

        //store server list
//        List<InetSocketAddress> serverList = new ArrayList<InetSocketAddress>();
//        servers = java.util.Collections.synchronizedList(serverList);
        //Start a thread to exchange server list
//        Thread exchange = new Thread(() -> Exchange.serverExchange(connection.exchangeInterval * 1000, servers));
//        exchange.start();

    //specify the keystore details (this can be specified as VM arguments as well)
	  //the keystore file contains an application's owncertificate and private key
	  System.setProperty("javax.net.ssl.keyStore","serverKeyStore/serverKeystore.jks");
	  //passowrd to access the private key from the keystore file
	  System.setProperty("javax.net.ssl.keyStorePassword","comp90015");
	  
	  //enable debugging to view the handshake and communication which happens between the SSLClient
	  System.setProperty("javax.net.debug","all");
	  try {
    //create SSL server socket  
		  	SSLServerSocketFactory sslserversocketfactory =(SSLServerSocketFactory) SSLServerSocketFactory
		  				.getDefault();
		  	SSLServerSocket sslserversocket = (SSLServerSocket) sslserversocketfactory
		  				.createServerSocket(9999);
      
		  	//Accept client connection
		  	SSLSocket sslsocket = (SSLSocket) sslserversocket.accept();
    
		  	//Create buffered reader to read input from the client  
		  	InputStream inputstream = sslsocket.getInputStream();
		  	InputStreamReader inputstreamreader =new InputStreamReader(inputstream);
		  	BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
	        
		  	String string = null;
		  	while((string = bufferedreader.readLine()) != null)
		  	{
		      System.out.println(string);
		      System.out.flush();
		  	}
	      	}
	  catch (Exception exception)
    {
      exception.printStackTrace();
    }
    }
}
