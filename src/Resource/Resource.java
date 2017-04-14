package Resource;

import java.util.List;

/**
 * Created by Yahang Wu on 2017/4/13.
 * COMP90015 Distributed System Project1 EZServer
 * Resource Class
 */
public class Resource {
    /*String name = "";
    String description = "";
    String[] tags;
    String uri;
    String channel = "";
    String owner = "";
    String ezserver = "EZShare";*/
	
	private String name;
	private String description;
	private List<String> tags;
	private String uri;
	private String channel;
	private String owner;
	private String ezserver;
	
	public Resource(String name, String description, List<String> tags, String uri, 
			        String channel, String owner, String ezserver){
		if(name == null || description == null || uri == null || tags == null || channel == null || owner == null || ezserver == null){
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
		
}
