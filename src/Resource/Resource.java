package Resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Created by Yahang Wu on 2017/4/13.
 * COMP90015 Distributed System Project1 EZServer
 * Resource Class
 */

public class Resource {

    private String name;
    private String description;
    private List<String> tags;
    private String uri;
    private String channel;
    private String owner;
    private String ezserver;

    /**
     * The Resource constructor
     *
     * @param name        the resource's name
     * @param description the resource's description
     * @param tags        the resource's the tags
     * @param uri         the resource's uri
     * @param channel     the resource's channel
     * @param owner       the resource's owner
     * @param ezserver    the resource's ezserver
     */
    public Resource(String name, String description, List<String> tags, String uri,
                    String channel, String owner, String ezserver) {
        if (name == null || description == null || uri == null || tags == null || channel == null || owner == null || ezserver == null) {
            //Just crashes at the moment.
            throw new IllegalArgumentException("Resource elements must not be null.");
        }
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.uri = uri;
        this.channel = channel;
        this.owner = owner;
        this.ezserver = ezserver;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getUri() {
        return uri;
    }

    public String getChannel() {
        return channel;
    }

    public String getOwner() {
        return owner;
    }

    public String getEzserver() {
        return ezserver;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * The method to convert the resource to a JsonObject
     *
     * @return the resource's JsonObject
     */
    public JsonObject toJsonObject() {

        JsonObject resourceJson = new JsonObject();
        JsonArray tagsArray = new JsonArray();

        for (String s : tags) {
            tagsArray.add(s);
        }
        resourceJson.addProperty("name", name);
        resourceJson.add("tags", tagsArray);
        resourceJson.addProperty("description", description);
        resourceJson.addProperty("uri", uri);
        resourceJson.addProperty("channel", channel);
        resourceJson.addProperty("owner", owner);
        resourceJson.addProperty("ezserver", ezserver);

        return resourceJson;
    }
}
