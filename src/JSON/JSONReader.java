package JSON;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;

import exceptions.InvalidServerException;

/**
 * Created by Yahang Wu on 2017/3/31.
 * COMP90015 Distributed System Project1 EZServer
 * This file provide the methods to read JSON file
 * required in the EZServer
 */

public class JSONReader {

    private JsonObject object;
    private JsonObject resource;

    /**
     * The constructor of the JSONReader
     *
     * @param jsonString the json string which is ready to be convert into
     *                   JsonObject
     */
    public JSONReader(String jsonString) {

        try {
            JsonParser parse = new JsonParser();
            object = (JsonObject) parse.parse(jsonString);
            //object = (JsonObject) parse.parse(new FileReader(fileName));
            if (object.has("resource")) {
                resource = object.get("resource").getAsJsonObject();
            } else if (object.has("resourceTemplate")) {
                resource = object.get("resourceTemplate").getAsJsonObject();
            } else
                resource = object;

        } catch (JsonIOException | JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * The constructor of the JSONReader
     *
     * @param object the already exist JsonObject ready to be analysed
     */
    public JSONReader(JsonObject object) {
        this.object = object;
        if (object.has("resource")) {
            resource = object.get("resource").getAsJsonObject();
        } else if (this.object.has("resourceTemplate")) {
            resource = object.get("resourceTemplate").getAsJsonObject();
        } else
            this.resource = object;
    }

    /**
     * The method to return the command
     *
     * @return the command string
     */
    public String getCommand() {
        return object.get("command").getAsString();
    }

    /**
     * The method to return the resource included in the complete jsonObject
     *
     * @return the resource JsonObject
     */
    public JsonObject getResources() {
        return resource;
    }

    /**
     * The method to return the resource name
     *
     * @return the resource name string
     */
    public String getResourceName() {
        return resource.get("name").getAsString();
    }

    /**
     * The method to return the resource description
     *
     * @return the resource description string
     */
    public String getResourceDescription() {
        return resource.get("description").getAsString();
    }

    /**
     * The method to return the resource uri
     *
     * @return the resource uri string
     */
    public String getResourceUri() {
        return resource.get("uri").getAsString();
    }

    /**
     * The method to return the resource channel
     *
     * @return the resource channel string
     */
    public String getResourceChannel() {
        return resource.get("channel").getAsString();
    }

    /**
     * The method to return the resource owner
     *
     * @return the resource owner string
     */
    public String getResourceOwner() {
        return resource.get("owner").getAsString();
    }

    /**
     * The method to return the resource ezserver
     *
     * @return the resource ezserver string
     */
    public String getResourceEZserver() {
        JsonElement server = resource.get("ezserver");
        if (server == null) {
            return "";
        } else {
            return server.toString();
        }
    }

    /**
     * The method to return the resource tags
     *
     * @return the resource tags string array
     */
    public String[] getResourceTags() {
        JsonArray array = resource.getAsJsonArray("tags");
        String[] tags = new String[array.size()];

        for (int i = 0; i < array.size(); i++) {
            tags[i] = array.get(i).getAsString();
        }
        return tags;
    }

    /**
     * The method to return the resource secret
     *
     * @return the secret string
     */
    public String getSecret() {
        return object.get("secret").getAsString();
    }

    /**
     * The method to return the resource relay
     *
     * @return the relay's value, true of false
     */
    public Boolean getRelay() {
        return object.get("relay").getAsBoolean();
    }

    /**
     * The method to return the resourceTemplate
     *
     * @return the resourceTemplate JsonObject
     */
    public JsonObject getResourceTemplate() {
        return object.get("resourceTemplate").getAsJsonObject();
    }

    /**
     * Server list contains two hostname and two ports
     * The first index of the array would contains the first hostname and port
     * The second index would contains the second
     *
     * @return the serverList String Array with has two dimensions
     * @throws InvalidServerException throw exceptions if serverList has invalid port number
     */
    public List<InetSocketAddress> getServerList() throws InvalidServerException {
        List<InetSocketAddress> serverList = new ArrayList<InetSocketAddress>();
        JsonArray list = object.get("serverList").getAsJsonArray();
        try {
            for (int i = 0; i < list.size(); i++) {
                JsonObject host = list.get(i).getAsJsonObject();
                String hostName = host.get("hostname").getAsString();
                int port = host.get("port").getAsInt();
                InetSocketAddress server = new InetSocketAddress(hostName, port);
                serverList.add(server);
            }
        } catch (IllegalArgumentException e) {
            //Thrown if port number invalid (if hostname invalid will be unresolved, should check).
            throw new InvalidServerException("Server entry in serverList has invalid port number.");
        }
        return serverList;
    }

    /**
     * The method to convert the server list to JsonObject
     *
     * @param servers       the server list
     * @param serverToShare the picked server to exchange server list
     * @return the server list exclude the server which is going to share with
     */
    public static JsonObject generateServerList(List<InetSocketAddress> servers, int serverToShare) {

        JsonArray serverList = new JsonArray();

        for (InetSocketAddress server : servers) {
            if (!server.equals(servers.get(serverToShare))) {
                JsonObject host = new JsonObject();
                host.addProperty("hostname", server.getAddress().getHostName());
                host.addProperty("port", server.getPort());
                serverList.add(host);
            }
        }

        JsonObject sendMessage = new JsonObject();
        sendMessage.addProperty("command", "EXCHANGE");
        sendMessage.add("serverList", serverList);
        return sendMessage;
    }

    /**
     * The method to check whether a string is a valid json string
     *
     * @param jsonInString the string needed to be checked whether it is a
     *                     json string.
     * @return true for the string is a json string
     */
    public static boolean isJSONValid(String jsonInString) {
        Gson gson = new Gson();
        try {
            gson.fromJson(jsonInString, Object.class);
            return true;
        } catch (com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }
}