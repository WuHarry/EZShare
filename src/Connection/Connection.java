package Connection;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.logging.Logger;

/**
 * Created by Yahang Wu on 2017/4/5.
 * COMP90015 Distributed System Project1 EZServer
 * This file provide the methods to read the command line
 * and get instruction from the command line
 * Also, it read the command line and convert it to the json string
 * in order to sent to the server
 */

public class Connection {

    private static final Logger logger =
            Logger.getLogger(Connection.class.getName());
    //Debug Mode, true is on, false is off.
    public static boolean debugSwitch = false;
    public static String command = "";

    //Client
    public String host = "1.1.1.1";
    public int port = 3000;
    
    
    //for secure connection
    public int sport = 3781;
    public String secure = "";
    public static boolean secureSwitch = false;
    
    private String channel = "";
    private String description = "";
    private String name = "";
    private String owner = "";
    private String secret = "";
    private String ezserver = null;
    private String servers = "";
    private String uri = "";
    private JsonArray tagsArray = new JsonArray();

    //Server config option
    public static String hostName = "localhost";
    public String connectionIntervalLimit = "";
    public int exchangeInterval = 600;
    public int serverPort = 4000;
    public String serverSecret = "";

    /**
     * The method to add and analyze the command options for the client
     * and config different parameters of the client and send the users' command
     * to the server as a json string
     *
     * @param args the command line arguments
     * @return commandObject.toString() the json string which contains
     * the command and attributes
     */
    public String clientCli(String[] args) {

        //To create the json object from the command line
        JsonObject commandObject = new JsonObject();

        Options options = new Options();

        options.addOption("channel", true, "channel");
        options.addOption("debug", false, "print debug information");
        options.addOption("description", true, "resource description");
        options.addOption("exchange", false,
                "exchange server list with server");
        options.addOption("fetch", false, "fetch resources from server");
        options.addOption("host", true,
                "server host, a domain name or IP address");
        options.addOption("name", true, "resource name");
        options.addOption("owner", true, "owner");
        options.addOption("port", true, "server port, an integer");
        options.addOption("publish", false, "publish resource on server");
        options.addOption("query", false, "query for resources from server");
        options.addOption("remove", false, "remove resource from server");
        options.addOption("secret", true, "secret");
        options.addOption("servers", true,
                "server list, host1:port1,host2:port2,...");
        options.addOption("share", false, "share resource on server");
        options.addOption("tags", true, "resource tags, tag1,tag2,tag3,...");
        options.addOption("uri", true, "resource URI");
        options.addOption("secure", false, "for SSL connection");
        options.addOption("sport", true, "for port number of SSL connection");
        options.addOption("subscribe", false, "to subscribe to a server");
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            //help(options);
            logger.warning("Wrong command line input;");
            System.out.println("Wrong command line input;");
        }

        assert cmd != null;

        if (cmd.hasOption("host")) {
            host = cmd.getOptionValue("host");
        }

        if (cmd.hasOption("port")) {
            port = Integer.parseInt(cmd.getOptionValue("port"));
        }
        //add by Danni Zhao 9 May 2017
        if (cmd.hasOption("sport")) {
            sport = Integer.parseInt(cmd.getOptionValue("sport"));
        }
        if (cmd.hasOption("secure")) {
            secureSwitch = true;
        }
        if (cmd.hasOption("debug")) {
            debugSwitch = true;
        }

        if (cmd.hasOption("secret")) {
            secret = cmd.getOptionValue("secret");
        }

        if (cmd.hasOption("name")) {
            name = cmd.getOptionValue("name");
        }

        if (cmd.hasOption("channel")) {
            channel = cmd.getOptionValue("channel");
        }

        if (cmd.hasOption("description")) {
            description = cmd.getOptionValue("description");
        }

        if (cmd.hasOption("tags")) {
            String tags = cmd.getOptionValue("tags");
            String[] array = tags.split(",");
            for (String str : array) {
                tagsArray.add(str);
            }
        }

        if (cmd.hasOption("uri")) {
            uri = cmd.getOptionValue("uri");
        }

        if (cmd.hasOption("owner")) {
            owner = cmd.getOptionValue("owner");
        }

        if (cmd.hasOption("servers")) {
            servers = cmd.getOptionValue("servers");
        }

        if (cmd.hasOption("publish") && commandObject.get("command") == null) {
            command = "PUBLISH";
            commandObject.addProperty("command", command);
            JsonObject resource = new JsonObject();
            resourceGenerator(resource);
            commandObject.add("resource", resource);

            return commandObject.toString();
        }

        if (cmd.hasOption("query") && commandObject.get("command") == null) {
            command = "QUERY";
            commandObject.addProperty("command", command);
            commandObject.addProperty("relay", true);
            JsonObject resourceTemplate = new JsonObject();
            resourceGenerator(resourceTemplate);
            commandObject.add("resourceTemplate", resourceTemplate);

            return commandObject.toString();
        }

        if (cmd.hasOption("remove") && commandObject.get("command") == null) {
            command = "REMOVE";
            commandObject.addProperty("command", command);
            JsonObject resource = new JsonObject();
            resourceGenerator(resource);
            commandObject.add("resource", resource);

            return commandObject.toString();
        }

        if (cmd.hasOption("share") && commandObject.get("command") == null) {
            command = "SHARE";
            commandObject.addProperty("command", command);
            commandObject.addProperty("secret", secret);
            JsonObject resource = new JsonObject();
            resourceGenerator(resource);
            commandObject.add("resource", resource);

            return commandObject.toString();
        }

        if (cmd.hasOption("exchange") && commandObject.get("command") == null) {
            command = "EXCHANGE";
            commandObject.addProperty("command", command);

            JsonArray serverList = new JsonArray();
            String[] list = servers.split(",");

            for (String host : list) {
                JsonObject server = new JsonObject();
                String[] hostAndPost = host.split(":");
                server.addProperty("hostname", hostAndPost[0]);
                server.addProperty("port", Integer.parseInt(hostAndPost[1]));
                serverList.add(server);
            }

            commandObject.add("serverList", serverList);

            System.out.println(commandObject.toString());
            return commandObject.toString();
        }

        if (cmd.hasOption("fetch") && commandObject.get("command") == null) {
            command = "FETCH";
            commandObject.addProperty("command", command);
            JsonObject resourceTemplate = new JsonObject();
            resourceGenerator(resourceTemplate);
            commandObject.add("resourceTemplate", resourceTemplate);

            return commandObject.toString();
        }
    /*    
        if (cmd.hasOption("subscribe"))
        {
        	
        }*/

        return null;
    }

    /**
     * The common method to generate the JsonObject resource
     *
     * @param resource the resource JsonObject
     */
    private void resourceGenerator(JsonObject resource) {
        resource.addProperty("name", name);
        resource.add("tags", tagsArray);
        resource.addProperty("description", description);
        resource.addProperty("uri", uri);
        resource.addProperty("channel", channel);
        resource.addProperty("owner", owner);
        resource.addProperty("ezserver", ezserver);
    }
    
    public static boolean getSecure(){
    	if (secureSwitch)
    			return true;
    	else return false;
    }
    /**
     * The method to add and analyze the command options for the server
     * and config different parameters of the server
     *
     * @param args the command line arguments
     */
    public void serverCli(String[] args) {

        //Generate long, random string for server to use as secret.
        //Might be a bit too long at moment.
        SecureRandom random = new SecureRandom();
        serverSecret = new BigInteger(130, random).toString(32);

        //debug switch
        boolean debugSwitch = false;

        Options options = new Options();

        options.addOption("advertisedhostname", true, "advertised hostname");
        options.addOption("connectionintervallimit", true,
                "connection interval limit in seconds");
        options.addOption("exchangeinterval", true,
                "exchange interval in seconds");
        options.addOption("port", true, "server port, an integer");
        options.addOption("secret", true, "secret");
        options.addOption("debug", false, "print debug information");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            //help(options);
            logger.warning("Wrong command line input;");
            System.out.println("Wrong command line input;");
        }

        assert cmd != null;
        
        //for secure connection
        if (cmd.hasOption("secure")) {
            secureSwitch = true;
            logger.info("secure connection established");
            
        }
        
        if (cmd.hasOption("debug")) {
            debugSwitch = true;
            logger.info("debug mode on");
        }

        if (cmd.hasOption("advertisedhostname")) {
            hostName = cmd.getOptionValue("advertisedhostname");
            if (debugSwitch) logger.info("Hostname changed to " + hostName);
        }

        if (cmd.hasOption("connectionintervallimit")) {
            connectionIntervalLimit =
                    cmd.getOptionValue("connectionintervallimit");
            if (debugSwitch) logger.info(
                    "connection interval limit changed to: " +
                            connectionIntervalLimit);
        }

        if (cmd.hasOption("exchangeinterval")) {
            exchangeInterval =
                    Integer.parseInt(cmd.getOptionValue("exchangeinterval"));
            if (debugSwitch) logger.info("Exchange interval changed to: " +
                    exchangeInterval);
        }

        if (cmd.hasOption("port")) {
            serverPort = Integer.parseInt(cmd.getOptionValue("port"));
            if (debugSwitch) logger.info("port set to: " + serverPort);
        }

        if (cmd.hasOption("secret")) {
            serverSecret = cmd.getOptionValue("secret");
            if (debugSwitch) logger.info("secret changed to: " + serverSecret);
        }
    }
}
