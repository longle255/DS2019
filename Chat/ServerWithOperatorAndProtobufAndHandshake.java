import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.io.*;

public class ServerWithOperatorAndProtobufAndHandshake {
    private ServerSocket server;
    private int clientCount = 0;
    private Vector<SocketHandler> clients = new Vector<SocketHandler>();

    public ServerWithOperatorAndProtobufAndHandshake(int port) {
        try {
            server = new ServerSocket(port);
            ServerOperator operator = new ServerOperator();
            operator.start();

            System.out.println("Server started");
            System.out.println("Waiting for a client ...");

            while (true) {
                Socket socket = server.accept();
                System.out.println("Client accepted");
                Thread handler = new SocketHandler(socket, ++clientCount);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        /*
        if (args.length != 1) {
            System.out.println("Usage: server [port]");
            System.exit(0);
        }
        ServerWithOperatorAndProtobuf server = new ServerWithOperatorAndProtobuf(Integer.parseInt(args[0]));
        */
        ServerWithOperatorAndProtobufAndHandshake server = new ServerWithOperatorAndProtobufAndHandshake(8080);
    }

    class SocketHandler extends Thread {
        private DataInputStream in;
        private DataOutputStream out;
        private Socket socket;
        private String prefix;
        private int clientId;

        public SocketHandler(Socket socket, int clientId) throws IOException {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.clientId = clientId;
            this.prefix = "Client[" + clientId + "]: ";
        }

        @Override
        public void run() {
            try {
                clients.add(this);
                int serverId = 1;

                Protocol.Handshake handshakeIn = Protocol.Handshake.parseDelimitedFrom(in);

                boolean someCondition = false;

                Protocol.Handshake.Builder handshakeOut = Protocol.Handshake.newBuilder();
                handshakeOut.setId(handshakeIn.getId());
                handshakeOut.setError(false);
                handshakeOut.build().writeDelimitedTo(out);

                if(someCondition){
                    System.out.println("Error in Connection "+handshakeIn.getId());
                } else {
                    System.out.println("Connection "+handshakeIn.getId() +" Established");
                    String msg = "";
                    while (!msg.equals("end")) {
                        try {
                            //msg = in.readUTF();
                            Protocol.Message fromClient = Protocol.Message.parseDelimitedFrom(in);
                            System.out.println(prefix + fromClient.getMsg() + ", Id: " + fromClient.getFr());
                            msg = fromClient.getMsg();

                            //out.writeUTF(msg);
                            Protocol.Message.Builder toClient = Protocol.Message.newBuilder();
                            toClient.setFr(serverId);
                            toClient.setTo(fromClient.getFr());
                            toClient.setMsg(fromClient.getMsg());
                            toClient.build().writeDelimitedTo(out);
                        } catch (IOException i) {
                            System.out.println(i);
                        }
                    }
                }

                System.out.println("Closing connection");
                clients.remove(this);
                // close connection
                socket.close();
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ServerOperator extends Thread {
        private BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        @Override
        public void run() {
            String command = "";
            while(true){
                try {
                    command = input.readLine();
                    if(command.equals("n_users")){
                        System.out.println("Users Connected: "+clients.size());
                    } else {
                        System.out.println("Invalid Command");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}