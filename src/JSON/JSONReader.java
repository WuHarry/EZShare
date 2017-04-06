package JSON;

/**
 * Created by Yahang Wu on 2017/3/31.
 * COMP90015 Distributed System Project1 EZServer
 * This file provide the methods to read JSON file
 * required in the EZServer
 */

import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.*;

public class JSONReader {

    private JsonObject object;
    private JsonObject resource;

    public JSONReader(String fileName) {

        try {
            JsonParser parse = new JsonParser();
            object = (JsonObject) parse.parse(new FileReader(fileName));
            resource = object.get("resource").getAsJsonObject();

        } catch (JsonIOException | JsonSyntaxException |
                FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public JSONReader(JsonObject object){
        this.object = object;
        this.resource = object.get("resource").getAsJsonObject();
    }

    public String getCommand() {
        return object.get("command").getAsString();
    }

    public JsonObject getResources() {
        return resource;
    }

    public String getResourceName() {
        return resource.get("name").getAsString();
    }

    public String getResourceDescription() {
        return resource.get("description").getAsString();
    }

    public String getResourceUri() {
        return resource.get("uri").getAsString();
    }

    public String getResourceChannel() {
        return resource.get("channel").getAsString();
    }

    public String getResourceOwner() {
        return resource.get("owner").getAsString();
    }

    public String getResourceEZserver() {
        JsonElement server = resource.get("ezserver");
        if (server == null) {
            return "";
        } else {
            return server.toString();
        }
    }

    public String[] getResourceTags() {
        JsonArray array = resource.getAsJsonArray("tags");
        String[] tags = new String[array.size()];

        for (int i = 0; i < array.size(); i++) {
            tags[i] = array.get(i).getAsString();
        }
        return tags;
    }

    public String getSecret() {
        return object.get("secret").getAsString();
    }

    public Boolean getRelay() {
        return object.get("relay").getAsBoolean();
    }

    public JsonObject getResourceTemplate() {
        return object.get("resourceTemplate").getAsJsonObject();
    }

    /* Server list contains two hostname and two ports
     * The first index of the array would contains the first hostname and port
     * The second index would contains the second
     */
    public String[][] getServerList() {
        String[][] serverList = new String[2][2];
        JsonArray list = object.get("serverList").getAsJsonArray();
        for (int i = 0; i < list.size(); i++) {
            JsonObject host = list.get(i).getAsJsonObject();
            serverList[i][0] = host.get("hostname").getAsString();
            serverList[i][1] = host.get("port").getAsString();
        }
        return serverList;
    }
}