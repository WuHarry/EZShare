import JSON.JSONReader;

/**
 * Created by harry on 2017/3/29.
 */

public class test {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        JSONReader newCommand = new JSONReader("test.json");
        System.out.println("Command = " + newCommand.getCommand());

        System.out.println("Resource Name = " + newCommand.getResourceName());
        System.out.println("Resource description = " +
                newCommand.getResourceDescription());
        System.out.println("Resource uri = " + newCommand.getResourceUri());
        System.out.println("Resource channel = " +
                newCommand.getResourceChannel());
        System.out.println("Resource owner = " + newCommand.getResourceOwner());
        System.out.println("Resource Ezserver = " +
                newCommand.getResourceEZserver());

        String[] temp = newCommand.getResourceTags();
        System.out.println("Resource Tags = {");
        for (String s : temp) {
            System.out.println("    " + s);
        }
        System.out.println("}");

        String[][] list = newCommand.getServerList();
        for (String[] s : list) {
            for (String t : s) {
                System.out.println(t);
            }
        }

        //Connection.clientCli(args);
        Connection.serverCli(args);
    }
}
