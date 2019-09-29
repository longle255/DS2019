import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private Socket socket;
    private ServerSocket server;

    public Server(int port) {
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");
            System.out.println("Waiting for a client ...");
            socket = server.accept();
            System.out.println("Client accepted");

            // takes input from the client socket
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            String msg = "";

            // reads message from client until "Over" is sent
            while (!msg.equals("end")) {
                try {
                    msg = in.readUTF();
                    System.out.println(msg);
                    out.writeUTF(msg);

                } catch (IOException i) {
                    System.out.println(i);
                }
            }
            System.out.println("Closing connection");
            // close connection
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: server [port]");
            System.exit(0);
        }
        Server server = new Server(Integer.parseInt(args[0]));
    }
}
