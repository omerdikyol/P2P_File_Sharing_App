public class Peer {
    // Instance variables
    private String ipAddress;
    private int port;

    public Peer(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return ipAddress + ":" + port;
    }
}
