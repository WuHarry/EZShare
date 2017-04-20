package EZShare;

import java.net.InetSocketAddress;
import java.util.List;

import JSON.JSONReader;
import Resource.HashDatabase;
import exceptions.InvalidServerException;
import exceptions.MissingComponentException;

public class Exchange {
	
	public static void exchange(JSONReader resource, HashDatabase db, List<InetSocketAddress> servers) throws MissingComponentException, InvalidServerException{
		
		//To conform with other command structure, move outside to Common method
		List<InetSocketAddress> serverList = resource.getServerList();
		if(serverList == null){
			throw new MissingComponentException("Trying to exchange without server list.");
		}
		
		for(InetSocketAddress server : serverList){
			if(server.isUnresolved()){
				throw new InvalidServerException("A server entry includes a hostname which cannot be resolved.");
			}
		}
		
		for(InetSocketAddress server : serverList){
			//Not sure if necessary, but ensures behaves as expected.
			synchronized(servers){
				if(!servers.contains(server)){
					servers.add(server);
				}
			}
		}
		
	}

}
