package JSON;

import com.google.gson.*;

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
     * @param jsonString the json string which is ready to be convert into
     *                   JsonObject
     */
    public JSONReader(String jsonString) {

        try {
            JsonParser parse = new JsonParser();
            object = (JsonObject) parse.parse(jsonString);
            //object = (JsonObject) parse.parse(new FileReader(fileName));
            if(object.has("resource"))
            {
                resource = object.get("resource").getAsJsonObject();
            } else if (object.has("resourceTemplate")){
                resource = object.get("resourceTemplate").getAsJsonObject();
            } else
                resource = object;

        } catch (JsonIOException | JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * The constructor of the JSONReader
     * @param object the already exist JsonObject ready to be analysed
     */
    public JSONReader(JsonObject object){
        this.object = object;
        if(object.has("resource"))
        {
            resource = object.get("resource").getAsJsonObject();
        } else if (this.object.has("resourceTemplate")){
            resource = object.get("resourceTemplate").getAsJsonObject();
        } else
            this.resource = object;
    }

    /**
     * The method to return the command
     * @return the command string
     */
    public String getCommand() {
        return object.get("command").getAsString();
    }

    /**
     * The method to return the resource included in the complete jsonObject
     * @return the resource JsonObject
     */
    public JsonObject getResources() {
        return resource;
    }

    /**
     * The method to return the resource name
     * @return the resource name string
     */
    public String getResourceName() {
        return resource.get("name").getAsString();
    }

    /**
     * The method to return the resource description
     * @return the resource description string
     */
    public String getResourceDescription() {
        return resource.get("description").getAsString();
    }

    /**
     * The method to return the resource uri
     * @return the resource uri string
     */
    public String getResourceUri() {
        return resource.get("uri").getAsString();
    }

    /**
     * The method to return the resource channel
     * @return the resource channel string
     */
    public String getResourceChannel() {
        return resource.get("channel").getAsString();
    }

    /**
     * The method to return the resource owner
     * @return the resource owner string
     */
    public String getResourceOwner() {
        return resource.get("owner").getAsString();
    }

    /**
     * The method to return the resource ezserver
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
     * @return the secret string
     */
    public String getSecret() {
        return object.get("secret").getAsString();
    }

    /**
     * The method to return the resource relay
     * @return the relay's value, true of false
     */
    public Boolean getRelay() {
        return object.get("relay").getAsBoolean();
    }

    /**
     * The method to return the resourceTemplate
     * @return the resourceTemplate JsonObject
     */
    public JsonObject getResourceTemplate() {
        return object.get("resourceTemplate").getAsJsonObject();
    }

    /**
     * Server list contains two hostname and two ports
     * The first index of the array would contains the first hostname and port
     * The second index would contains the second
     * @return the serverlist String Array with has two dimensions
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