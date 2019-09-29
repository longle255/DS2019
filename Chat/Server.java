import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket server;
    private int clientCount = 0;

    public Server(int port) {
        try {
            server = new ServerSocket(port);
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
        if (args.length != 1) {
            System.out.println("Usage: server [port]");
            System.exit(0);
        }
        Server server = new Server(Integer.parseInt(args[0]));
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
            this.prefix = "Client[" + clientId + "]:";
        }

        @Override
        public void run() {
            try {
                String msg = "";
                while (!msg.equals("end")) {
                    try {
                        msg = in.readUTF();
                        System.out.println(prefix + msg);
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
    }


}
