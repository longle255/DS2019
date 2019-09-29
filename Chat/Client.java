// A Java program for a Client 

import java.io.*;
import java.net.Socket;

public class Client {
    // initialize socket and input output streams
    private Socket socket = null;
    private BufferedReader input = null;
    private DataOutputStream out = null;
    private DataInputStream in = null;

    // constructor to put ip address and port 
    public Client(String address, int port) {
        // establish a connection
        try {
            socket = new Socket(address, port);
            System.out.println("Connected");
            // takes input from terminal
            input = new BufferedReader(new InputStreamReader(System.in));
            // sends output to the socket
            out = new DataOutputStream(socket.getOutputStream());
            // take response from the socket
            in = new DataInputStream(socket.getInputStream());


            // string to read message from input
            String msg = "";
            // keep reading until "Over" is input
            while (!msg.equals("end")) {
                msg = input.readLine();
                out.writeUTF(msg);
                msg = in.readUTF();
                System.out.println("Server response:" + msg);
            }
            // close the connection
            input.close();
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        Client client = new Client("127.0.0.1", 5000);
    }
} 