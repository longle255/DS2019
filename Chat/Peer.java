import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Peer {
    private Server server;
    private List<Client> peers = new ArrayList();
    private int peerId;

    public Peer(int port, int peerId) {
        this.peerId = peerId;
        this.server = new Server(port);
        new Thread(() -> server.listen()).start();
    }

    public static void main(String[] args) throws InterruptedException {
//       command <this port> <this id> <peer1 host> <peer1 port> [<peer2 host> <peer2 port>]
        int port = Integer.parseInt(args[0]);
        int clientId = Integer.parseInt(args[1]);
        Peer app = new Peer(port, clientId);

        // wait a bit for other peers to start
        Thread.sleep(5000);

        int i = 2;
        while (i < args.length) {
            app.connect(args[i], Integer.parseInt(args[i + 1]));
            i += 2;
        }

        app.start();
    }

    public void connect(String host, int port) {
        Client peer = new Client(peerId);
        peer.connect(host, port);
        peers.add(peer);
        Thread receiverThread = new Thread(() -> {
            while (true) {
                try {
                    Protocol.Message msg = Protocol.Message.parseDelimitedFrom(peer.getSocketInputStream());
                    if (msg.getTo() == this.peerId) {
                        System.out.println("[Client] [From:" + msg.getFr() + "]: " + msg.getMsg());
                    } else {
                        System.out.println("[Server] [Forwarding] [From:" + msg.getFr() + "]: " + msg.getMsg());
                        this.server.send(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        receiverThread.start();
    }

    public void start() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            String text = input.readLine();
            while (!text.equals("end")) {
                String tokens[] = text.split(" ");
                int buddyId;
                try {
                    buddyId = Integer.parseInt(tokens[0]);
                } catch (Exception e) {
                    System.out.println("msg format: <id> <msg>");
                    continue;
                }
                Protocol.Message.Builder msg = Protocol.Message.newBuilder();
                msg.setFr(peerId);
                msg.setTo(buddyId);
                msg.setMsg(text.substring(text.indexOf(" ") + 1));
                this.server.send(msg.build());
                text = input.readLine();
            }
            // close the connection
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class Client {
        // initialize socket and input output streams
        private Socket socket = null;
        private BufferedReader input = null;
        private DataOutputStream out = null;
        private DataInputStream in = null;
        private int clientId;

        // constructor to put ip address and port
        public Client(int clientId) {
            // establish a connection
            this.clientId = clientId;
        }

        public DataInputStream getSocketInputStream() {
            return in;
        }

        public void connect(String address, int port) {
            try {
                socket = new Socket(address, port);
                System.out.println("[Client] Connected to server [" + address + ":" + port + "]");
                // takes input from terminal
                input = new BufferedReader(new InputStreamReader(System.in));
                // sends output to the socket
                out = new DataOutputStream(socket.getOutputStream());
                // take response from the socket
                in = new DataInputStream(socket.getInputStream());

                Protocol.Handshake.Builder handshakeOut = Protocol.Handshake.newBuilder();
                handshakeOut.setId(clientId);
                handshakeOut.setError(false);
                handshakeOut.build().writeDelimitedTo(out);

                Protocol.Handshake handshakeIn = Protocol.Handshake.parseDelimitedFrom(in);
                if (handshakeIn.getError()) {
                    System.out.println("[Client] Could not Connect with the Server");
                    return;
                }
                System.out.println("[Client] Connection Established to server [" + address + ":" + port + "]");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public class Server {
        private ServerSocket server;
        private Map<Integer, SocketHandler> activeClients = new HashMap<>();
        private Map<Integer, List> allPendingMsgs = new HashMap<>();

        public Server(int port) {
            try {
                server = new ServerSocket(port);
                System.out.println("[Server] Server started at port [" + port + "]");
                System.out.println("[Server] Waiting for a client ...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void listen() {
            try {
                while (true) {
                    Socket socket = server.accept();
                    System.out.println("[Server] Connection accepted");
                    new SocketHandler(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void send(Protocol.Message message) throws IOException {
            SocketHandler dest = activeClients.get(message.getTo());
            List<DataOutputStream> peers = new ArrayList<>();
            if (dest == null) {
                peers = activeClients.values().stream()
                        .filter(socketHandler -> !message.getHopsList().contains(socketHandler.clientId))
                        .map(SocketHandler::getOut).collect(Collectors.toList());
            } else {
                peers.add(dest.out);
            }
            for (DataOutputStream out : peers) {
                message.writeDelimitedTo(out);
            }
        }

        class SocketHandler extends Thread {
            private DataInputStream in;
            private DataOutputStream out;
            private Socket socket;
            private int clientId;

            public SocketHandler(Socket socket) throws IOException {
                this.socket = socket;
                this.in = new DataInputStream(socket.getInputStream());
                this.out = new DataOutputStream(socket.getOutputStream());
                this.handshake();
            }

            public DataOutputStream getOut() {
                return out;
            }

            public void disconnect() {
                System.out.println("[Server] Closing connection");
                activeClients.remove(this.clientId);
                try {
                    socket.close();
                    in.close();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            private void handshake() throws IOException {
                Protocol.Handshake handshakeIn = Protocol.Handshake.parseDelimitedFrom(in);
                this.clientId = handshakeIn.getId();
                activeClients.put(this.clientId, this);

                Protocol.Handshake.Builder handshakeOut = Protocol.Handshake.newBuilder();
                handshakeOut.setId(handshakeIn.getId());
                handshakeOut.setError(false);
                handshakeOut.build().writeDelimitedTo(out);
                System.out.println("[Server] Connection Established for client " + handshakeIn.getId());

                List pendingMsgs = allPendingMsgs.get(this.clientId);
                if (pendingMsgs != null && pendingMsgs.size() > 0) {
                    while (pendingMsgs.size() > 0) {
                        Protocol.Message msg = (Protocol.Message) pendingMsgs.remove(0);
                        msg.writeDelimitedTo(out);
                    }
                }
            }
        }
    }
}
