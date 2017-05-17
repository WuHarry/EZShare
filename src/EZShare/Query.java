package EZShare;

import JSON.JSONReader;
import Resource.HashDatabase;
import Resource.Resource;
import com.google.gson.JsonObject;
import Exceptions.InvalidResourceException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Yahang Wu on 2017/4/16.
 * COMP90015 Distributed System Project1 EZServer
 * Server query function to query resources
 */
class Query {

    private static Logger logger = Logger.getLogger(Query.class.getName());

    /**
     * The query function of the server
     *
     * @param resource The resources should be queried.
     * @param db       The database the resource should be checked.
     * @return return all the resources that match the request
     * @throws InvalidResourceException If the resource supplied contains illegal fields, this is thrown.
     */
    static Set<Resource> query(JSONReader resource, HashDatabase db) throws InvalidResourceException {
        String name = resource.getResourceName();
        String description = resource.getResourceDescription();
        String channel = resource.getResourceChannel();
        String owner = resource.getResourceOwner();
        String uri = resource.getResourceUri();
        String[] tags = resource.getResourceTags();
        boolean relay = resource.getRelay();

        Set<Resource> resources = new HashSet<Resource>();

        //Check strings etc. are valid
        if (!Common.validateResource(name, description, tags, uri, channel, owner)) {
            throw new InvalidResourceException("Trying to query Resource with illegal fields.");
        }

        String tag = "";
        for (String t : tags) {
            tag += t;
        }
        if (db.channelLookup(channel) == null) return null;
        resources.addAll(db.channelLookup(channel));
        //remove owner doesn't match
        if (!owner.equals("")) {
            resources.removeIf(resource1 -> !resource1.getOwner().equals(owner));
        }
        //remove tags doesn't match
        if (!tag.equals("")) {
            resources.removeIf(resource1 -> !Arrays.equals(resource1.getTags().toArray(), tags));
        }
        //remove uri doesn't match
        if (!uri.equals("")) {
            resources.removeIf(resource1 -> !resource1.getUri().equals(uri));
        }
        //remove name doesn't match
        if (!name.equals("")) {
            resources.removeIf(resource1 -> !resource1.getName().contains(name));
        }
        //remove description doesn't match
        if (!description.equals("")) {
            resources.removeIf(resource1 -> !resource1.getDescription().contains(description));
        }

        if (!relay) {
            hideOwner(resources);
            return resources;
        } else {
            relay(resource, resources);
            hideOwner(resources);
            return resources;
        }
    }

    /**
     * The method to hide owner, if the owner is not ""
     *
     * @param resources the response resources
     */
    private static void hideOwner(Set<Resource> resources) {
        for (Resource r : resources) {
            if (!r.getOwner().equals("")) r.setOwner("*");
        }
    }

    private static void relay(JSONReader resource, Set<Resource> resources) {
        String name;
        String description;
        List<String> tags;
        String uri;
        String channel;
        String owner;
        String ezserver;

        //Generate new Json Command
        JsonObject queryCommand = resource.getJsonObject();
        queryCommand.addProperty("relay", false);
        JsonObject resourceTemplate = resource.getResourceTemplate();
        resourceTemplate.addProperty("owner", "");
        resourceTemplate.addProperty("channel", "");
        queryCommand.add("resourceTemplate", resourceTemplate);

        for (InetSocketAddress s : Server.servers) {
            String ip = s.getAddress().getHostName();
            int port = s.getPort();
            try (Socket socket = new Socket(ip, port)) {
                //input stream
                DataInputStream input =
                        new DataInputStream(socket.getInputStream());
                //output stream
                DataOutputStream output =
                        new DataOutputStream(socket.getOutputStream());

                output.writeUTF(queryCommand.toString());
                logger.fine("[SENT] - " + queryCommand.toString());
                output.flush();

                while (true) {
                    if (input.available() > 0) {
                        String message = input.readUTF();
                        logger.fine("[RECEIVE] - " + message);
                        JSONReader response = new JSONReader(message);
                        if (message.contains("response")) continue;
                        if (message.contains("resultSize")) break;
                        if (response.getResourceName() != null) {
                            name = response.getResourceName();
                            owner = response.getResourceOwner();
                            description = response.getResourceDescription();
                            channel = response.getResourceChannel();
                            uri = response.getResourceUri();
                            ezserver = response.getResourceEZserver();
                            tags = Arrays.asList(response.getResourceTags());

                            Resource temp = new Resource(name, description, tags, uri, channel, owner, ezserver);
                            resources.add(temp);
                        }
                    }
                }
                socket.close();
            } catch (UnknownHostException e1) {
                logger.warning(e1.getLocalizedMessage());
            } catch (IOException e2) {
                logger.warning(e2.getMessage());
            }
        }
    }
}
