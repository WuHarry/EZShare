/**
 * Created by Yahang Wu on 2017/3/30.
 * COMP90015 Distributed System Project1 EZServer
 */

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

import com.google.gson.*;

public class JSONReader {

    private JsonObject object;
    private JsonObject resource;

    public JSONReader(String fileName) {

        try {
            JsonParser parse = new JsonParser();
            object = (JsonObject) parse.parse(new FileReader(fileName));
            resource = object.get("resource").getAsJsonObject();

        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    String getCommand() {
        return object.get("command").getAsString();
    }

    JsonObject getResources() {
        return resource;
    }

    String getResourceName() {
        return resource.get("name").getAsString();
    }

    String getResourceDescription() {
        return resource.get("description").getAsString();
    }

    String getResourceUri() {
        return resource.get("uri").getAsString();
    }

    String getResourceChannel() {
        return resource.get("channel").getAsString();
    }

    String getResourceOwner() {
        return resource.get("owner").getAsString();
    }

    String getResourceEZserver() {
        JsonElement server = resource.get("ezserver");
        if (server == null) {
            return "";
        } else {
            return server.toString();
        }
    }

    String[] getResourceTags() {
        JsonArray array = resource.getAsJsonArray("tags");
        String[] tags = new String[array.size()];

        for (int i = 0; i < array.size(); i++) {
            tags[i] = array.get(i).getAsString();
        }
        return tags;
    }

    String getSecret() {
        return object.get("secret").getAsString();
    }

    Boolean getRelay() {
        return object.get("relay").getAsBoolean();
    }

    JsonObject getResourceTemplate() {
        return object.get("resourceTemplate").getAsJsonObject();
    }

    /* Server list contains two hostname and two ports
     * The first index of the array would contains the first hostname and port
     * The second index would contains the second
     */
    String[][] getServerList() {
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