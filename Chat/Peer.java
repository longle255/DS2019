public class Peer {
    private ServerWithOperatorAndProtobufAndHandshake server;
    private ClientWithHandshake selfConn;
    private ClientWithHandshake otherConn;

    public Peer(int port, int clientId, int otherPort) {

        Thread serverThread = new Thread(() -> this.server = new ServerWithOperatorAndProtobufAndHandshake(port));
        serverThread.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.selfConn = new ClientWithHandshake(clientId);
        this.selfConn.connect("127.0.0.1", port);
        this.otherConn = new ClientWithHandshake(clientId);
        this.otherConn.connect("127.0.0.1", otherPort);
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        int clientId = Integer.parseInt(args[1]);
        int otherPort = Integer.parseInt(args[2]);
        Peer app = new Peer(port, clientId, otherPort);
    }
}
