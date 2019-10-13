import java.io.*;
import java.net.Socket;
import java.util.Random;

public class ClientWithHandshake {
    // initialize socket and input output streams
    private Socket socket = null;
    private BufferedReader input = null;
    private DataOutputStream out = null;
    private DataInputStream in = null;

    // constructor to put ip address and port
    public ClientWithHandshake(String address, int port) {
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

            int serverId = 1;
            int myId = new Random().nextInt(100);

            Protocol.Handshake.Builder handshakeOut = Protocol.Handshake.newBuilder();
            handshakeOut.setId(myId);
            handshakeOut.setError(false);
            handshakeOut.build().writeDelimitedTo(out);

            Protocol.Handshake handshakeIn = Protocol.Handshake.parseDelimitedFrom(in);
            if(handshakeIn.getError()){
                System.out.println("Could not Connect with the Server");
            } else {
                System.out.println("Connection Established");
                // string to read message from input
                String msg = "";
                // keep reading until "end" is input
                while (!msg.equals("end")) {
                    msg = input.readLine();

                    //out.writeUTF(msg);
                    Protocol.Message.Builder toServer = Protocol.Message.newBuilder();
                    toServer.setFr(myId);
                    toServer.setTo(serverId);
                    toServer.setMsg(msg);
                    toServer.build().writeDelimitedTo(out);

                    //msg = in.readUTF();
                    Protocol.Message fromServer = Protocol.Message.parseDelimitedFrom(in);

                    System.out.println("Server Message: " + fromServer.getMsg() + ", Server Id: " + fromServer.getFr());
                }
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
        ClientWithHandshake client = new ClientWithHandshake("127.0.0.1", 8080);
    }
}