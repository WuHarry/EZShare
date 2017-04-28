package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import Connection.Connection;
import JSON.JSONReader;
import Resource.HashDatabase;
import com.google.gson.JsonObject;
import exceptions.InvalidServerException;
import exceptions.MissingComponentException;

public class Exchange {

    private static Logger logger = Logger.getLogger(Exchange.class.getName());

    static void exchange(JSONReader resource, List<InetSocketAddress> servers) throws MissingComponentException, InvalidServerException {

        //To conform with other command structure, move outside to Common method
        List<InetSocketAddress> serverList = resource.getServerList();
        if (serverList == null) {
            throw new MissingComponentException("Trying to exchange without server list.");
        }

        for (InetSocketAddress server : serverList) {
            if (server.isUnresolved()) {
                throw new InvalidServerException("A server entry includes a hostname which cannot be resolved.");
            }
        }

        for (InetSocketAddress server : serverList) {
            //Not sure if necessary, but ensures behaves as expected.
            synchronized (servers) {
                if (!servers.contains(server)) {
                    servers.add(server);
                }
            }
        }
    }

    static void serverExchange(int exchangeInterval, List<InetSocketAddress> servers){

        while(true){
            try{
                Thread.sleep(exchangeInterval);
                if (!servers.isEmpty()){
                    //pick the random server to exchange
                    int serverToShare = (int)(Math.random()*servers.size());
                    String serverIP = servers.get(serverToShare).getAddress().toString();
                    int serverPort = servers.get(serverToShare).getPort();
                    try(Socket socket = new Socket(serverIP, serverPort)){
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

                        while(true){
                            if(input.available()>0){
                                String message = input.readUTF();
                                logger.fine("[RECEIVE] - " + message);
                                break;
                            }
                        }
                    } catch (UnknownHostException e) {
                        servers.remove(serverToShare);
                        logger.info("Server " + serverIP + ":" + serverPort +" unreachable.");
                        logger.info("[REMOVE] - Removed server " + serverIP + ":" +serverPort);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
