import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerWithOperatorAndProtobufAndHandshake {
    private ServerSocket server;
    //    private Vector<SocketHandler> clients = new Vector<SocketHandler>();
    private Map<Integer, SocketHandler> activeClients = new HashMap<>();

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
                Thread handler = new SocketHandler(socket);
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

        public SocketHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            try {
//                clients.add(this);
                int serverId = 0;

                Protocol.Handshake handshakeIn = Protocol.Handshake.parseDelimitedFrom(in);
                this.clientId = handshakeIn.getId();
                activeClients.put(this.clientId, this);

                Protocol.Handshake.Builder handshakeOut = Protocol.Handshake.newBuilder();
                handshakeOut.setId(handshakeIn.getId());
                handshakeOut.setError(false);
                handshakeOut.build().writeDelimitedTo(out);


                System.out.println("Connection " + handshakeIn.getId() + " Established");
                String msg = "";
                while (!msg.equals("end")) {
                    try {
                        //msg = in.readUTF();
                        Protocol.Message fromClient = Protocol.Message.parseDelimitedFrom(in);
                        msg = fromClient.getMsg();
                        if (msg == null) {
                            activeClients.remove(this.clientId);
                            return;
                        }

                        System.out.println("Msg from client " + fromClient.getFr() + " to client " + fromClient.getTo() + ":" + fromClient.getMsg());

                        SocketHandler other = activeClients.get(fromClient.getTo());
                        if (other == null) {
                            Protocol.Message.Builder toClient = Protocol.Message.newBuilder();
                            toClient.setFr(serverId);
                            toClient.setTo(fromClient.getFr());
                            toClient.setMsg("Clien with id " + fromClient.getTo() + " does not exist");
                            toClient.build().writeDelimitedTo(out);
                            continue;
                        }
                        DataOutputStream otherOut = other.out;
                        //out.writeUTF(msg);
                        Protocol.Message.Builder toClient = Protocol.Message.newBuilder();
                        toClient.setFr(fromClient.getFr());
                        toClient.setTo(fromClient.getTo());
                        toClient.setMsg(fromClient.getMsg());
                        toClient.build().writeDelimitedTo(otherOut);
                    } catch (IOException i) {
                        System.out.println(i);
                    }
                }


                System.out.println("Closing connection");
                activeClients.remove(this.clientId);
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
            while (true) {
                try {
                    command = input.readLine();
                    if (command.equals("n_users")) {
                        System.out.println("Users Connected: " + activeClients.keySet().size());
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