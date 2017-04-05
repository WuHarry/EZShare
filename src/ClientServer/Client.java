package ClientServer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by harry on 2017/3/31.
 */
public class Client {

    //ip and port
    private static String ip = "localhost";
    private static int port = 3000;

    public static void main(String[] args){
        //new client socket
        try(Socket socket = new Socket(ip, port)){
            //input stream
            DataInputStream input = new DataInputStream(socket.getInputStream());
            //output stream
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            output.writeUTF("I want to connect!");
            output.flush();

            while(true){
                if(input.available() > 0){
                    String message = input.readUTF();
                    System.out.println(message);
                }
            }

        }catch (IOException e){
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, e.getMessage());
        }
    }
}
