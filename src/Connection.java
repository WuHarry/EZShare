/**
 * Created by Yahang Wu on 2017/4/5.
 * COMP90015 Distributed System Project1 EZServer
 * This file provide the methods to read the command line
 * and get instruction from the command line
 */

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;

public class Connection {

    /**
     * @param args the command line arguments
     */
    public static void clientCli(String[] args) {

        String channel = "";
        String description = "";
        String host = "";
        String name = "";
        String owner = "";
        int port = 0;
        String secret = "";
        String server = "";
        String tags = "";
        String uri = "";

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

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            //help(options);
            System.out.println("I am here.");
        }

        assert cmd != null;

        if (cmd.hasOption("channel")) {
            channel = cmd.getOptionValue("channel");
            System.out.println("channel succeed: " + channel);
        } else {
            channel = "The user does not provide channel";
            System.out.println(channel);
        }

        if (cmd.hasOption("debug")) {
            System.out.println("debug succeed");
        } else {
            System.out.println("debug failed");
        }

        if (cmd.hasOption("description")) {
            description = cmd.getOptionValue("description");
            System.out.println("description succeed: " + description);
        } else {
            description = "The user does not provide description";
            System.out.println(description);
        }

        if (cmd.hasOption("exchange")) {
            System.out.println("exchange succeed");
        } else {
            System.out.println("exchange failed");
        }

        if (cmd.hasOption("fetch")) {
            System.out.println("fetch succeed");
        } else {
            System.out.println("fetch failed");
        }

        if (cmd.hasOption("host")) {
            host = cmd.getOptionValue("host");
            System.out.println("host succeed: " + host);
        } else {
            host = "The user does not provide host";
            System.out.println(host);
        }

        if (cmd.hasOption("name")) {
            name = cmd.getOptionValue("name");
            System.out.println("name succeed: " + name);
        } else {
            name = "The user does not provide name";
            System.out.println(name);
        }

        if (cmd.hasOption("owner")) {
            owner = cmd.getOptionValue("owner");
            System.out.println("owner succeed: " + owner);
        } else {
            owner = "The user does not provide owner";
            System.out.println(owner);
        }

        if (cmd.hasOption("port")) {
            port = Integer.parseInt(cmd.getOptionValue("port"));
            System.out.println("port succeed: " + port);
        } else {
            System.out.println("port failed, port remain: " + port);
        }

        if (cmd.hasOption("publish")) {
            System.out.println("publish succeed");
        } else {
            System.out.println("publish failed");
        }

        if (cmd.hasOption("query")) {
            System.out.println("query succeed");
        } else {
            System.out.println("query failed");
        }

        if (cmd.hasOption("remove")) {
            System.out.println("remove succeed");
        } else {
            System.out.println("remove failed");
        }

        if (cmd.hasOption("secret")) {
            secret = cmd.getOptionValue("secret");
            System.out.println("secret succeed: " + secret);
        } else {
            secret = "The user does not provide secret";
            System.out.println(secret);
        }

        if (cmd.hasOption("server")) {
            server = cmd.getOptionValue("server");
            System.out.println("server succeed: " + server);
        } else {
            server = "The user does not provide server";
            System.out.println(server);
        }

        if (cmd.hasOption("share")) {
            System.out.println("share succeed");
        } else {
            System.out.println("share failed");
        }

        if (cmd.hasOption("tags")) {
            tags = cmd.getOptionValue("tags");
            System.out.println("tags succeed: " + tags);
        } else {
            tags = "The user does not provide tags";
            System.out.println(tags);
        }

        if (cmd.hasOption("uri")) {
            uri = cmd.getOptionValue("uri");
            System.out.println("uri succeed: " + uri);
        } else {
            uri = "The user does not provide uri";
            System.out.println(uri);
        }
    }

    /**
     * @param args the command line arguments
     */

    public static void serverCli(String[] args) {

        String advertisedHostname = "EZShaer Server";
        String connectionIntervalLimit = "";
        int exchangeInterval = 600;
        int port = 0;
        String secret = "kfjdskfjaskldfjkalsjfk";

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
            System.out.println("I am here.");
        }

        assert cmd != null;

        if (cmd.hasOption("advertisedhostname")) {
            advertisedHostname += cmd.getOptionValue("advertisedhostname");
            System.out.println("advertisedhostname succeed: " +
                    advertisedHostname);
        } else {
            System.out.println(advertisedHostname);
        }

        if (cmd.hasOption("connectionintervallimit")) {
            connectionIntervalLimit =
                    cmd.getOptionValue("connectionintervallimit");
            System.out.println("connectionintervallimit succeed: " +
                    connectionIntervalLimit);
        } else {
            connectionIntervalLimit =
                    "The user does not provide connection interval limit";
            System.out.println(connectionIntervalLimit);
        }

        if (cmd.hasOption("exchangeinterval")) {
            exchangeInterval +=
                    Integer.parseInt(cmd.getOptionValue("exchangeinterval"));
            System.out.println("Exchange interval succeed: " +
                    exchangeInterval);
        } else {
            System.out.println("default exchange interval: " +
                    exchangeInterval);
        }

        if (cmd.hasOption("port")) {
            port = Integer.parseInt(cmd.getOptionValue("port"));
            System.out.println("port succeed: " + port);
        } else {
            System.out.println("port failed, port remain: " + port);
        }

        if (cmd.hasOption("secret")) {
            secret += cmd.getOptionValue("secret");
            System.out.println("secret succeed: " + secret);
        } else {
            System.out.println(secret);
        }

        if (cmd.hasOption("debug")) {
            System.out.println("debug succeed");
        } else {
            System.out.println("debug failed");
        }
    }

}
