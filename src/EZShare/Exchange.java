package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Logger;

import Connection.Connection;
import JSON.JSONReader;
import com.google.gson.JsonObject;
import Exceptions.InvalidServerException;
import Exceptions.MissingComponentException;

class Exchange {

    private static Logger logger = Logger.getLogger(Exchange.class.getName());

    /**
     * The exchange function of the server to receive serverList and add the
     * non-exist serverList to the server's list
     *
     * @param resource the command resource that client sent to the server
     * @param servers  the server's list of servers
     * @throws MissingComponentException throws when the serverList received is null
     * @throws InvalidServerException    throws when the serverList contains unresolved server
     */
    static void exchange(JSONReader resource, List<InetSocketAddress> servers) throws MissingComponentException, InvalidServerException {

        //To conform with other command structure, move outside to Common method
        List<InetSocketAddress> serverList = resource.getServerList();
        if (serverList == null) {
            throw new MissingComponentException("missing server list - Trying to exchange without server list.");
        }

        for (InetSocketAddress server : serverList) {
            if (server.isUnresolved()) {
                throw new InvalidServerException("A server entry includes a hostname which cannot be resolved.");
            }
        }

        for (InetSocketAddress server : serverList) {
            //Not sure if necessary, but ensures behaves as expected.
            synchronized (servers) {
                if (!servers.contains(server) &&
                        (!server.getAddress().getHostName().equals(Connection.hostName) &&
                        server.getPort() != Server.port)) {
                    servers.add(server);
                }
            }
        }
    }

    /**
     * The function to exchange serverList between servers, pick a random server
     * to exchange serverList. The method packed all the server to a JsonObject
     * and send the server list as a Json String
     *
     * @param exchangeInterval the exchange time period
     * @param servers          the server's list
     */
    static void serverExchange(int exchangeInterval, List<InetSocketAddress> servers, boolean isSecure) {

        while (true) {
            try {
                Thread.sleep(exchangeInterval);
                if (!servers.isEmpty()) {
                    //pick the random server to exchange
                    int serverToShare = (int) (Math.random() * servers.size());
                    String serverIP = servers.get(serverToShare).getAddress().getHostName();
                    int serverPort = servers.get(serverToShare).getPort();
                    try {
                        Socket socket;
                        if (isSecure){
                            socket = Common.initClientSSL().createSocket(serverIP, serverPort);
                        }else{
                            socket = new Socket(serverIP, serverPort);
                        }
                        //input stream
                        DataInputStream input =
                                new DataInputStream(socket.getInputStream());
                        //output stream
                        DataOutputStream output =
                                new DataOutputStream(socket.getOutputStream());

                        JsonObject serverList = JSONReader.generateServerList(servers, serverToShare);

                        output.writeUTF(serverList.toString());
                        logger.fine("[SENT] - " + serverList.toString());
                        output.flush();

                        while (true) {
                            try {
                                String message = input.readUTF();
                                logger.fine("[RECEIVE] - " + message);
                                break;
                            } catch (SocketTimeoutException e){
                                //just aimed to check weather the input is empty
                            }
                        }
                        socket.close();
                    } catch (IOException e) {
                        servers.remove(serverToShare);
                        logger.info("Server " + serverIP + ":" + serverPort + " unreachable.");
                        logger.info("[REMOVE] - Removed server " + serverIP + ":" + serverPort);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
