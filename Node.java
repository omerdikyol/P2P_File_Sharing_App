public class Node {
    // Instance variables
    private String ipAddress;
    private int port;
    private String sharedSecret;
    private String sharedFolderPath;

    public Node(String ipAddress, int port, String sharedSecret, String sharedFolderPath) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.sharedSecret = sharedSecret;
        this.sharedFolderPath = sharedFolderPath;
    }

    // Getters and Setters
    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public String getSharedFolderPath() {
        return sharedFolderPath;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "Node:" +
                "ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", sharedSecret='" + sharedSecret + '\'' +
                ", sharedFolderPath='" + sharedFolderPath + '\'';
    }
}
