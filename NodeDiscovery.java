import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NodeDiscovery {
    // Node class and socket variables
    private Node localNode;
    private DatagramSocket socket;
    private DatagramSocket discoverySocket;

    // Thread pool for scheduling tasks
    private ScheduledExecutorService executorService;

    // Callbacks for handling events
    private Consumer<String> onNodeDiscoveredCallback;
    private Consumer<String> onFileBroadcastCallback;
    private Consumer<String> onDisconnectCallback;
    private Consumer<String> onDeleteCallback;

    // Maps to keep track of files and peers
    private Map<String, List<Peer>> filePeersMap = new ConcurrentHashMap<>();
    private Map<String, FileMetadata> fileHashMap = new ConcurrentHashMap<>();

    // Set to keep track of recently disconnected nodes
    private Set<String> recentlyDisconnectedNodes = new HashSet<>();

    // Constants
    private static final int BUFFER_SIZE = 1024; // 1 KB
    private static final int CHUNK_SIZE = 512 * 1024; // 512 KB
    private static final int MAX_UDP_PACKET_SIZE = 8192; // 8 KB
    private static final int BROADCAST_PORT = 5000; // port for broadcasting

     // A map to keep track of connected nodes
    private Set<String> connectedNodes = Collections.synchronizedSet(new HashSet<>());

    // Call this method when a node connects
    private void addNode(String nodeInfoString) {
        synchronized (connectedNodes) {
            if (connectedNodes.contains(nodeInfoString)) {
                return;
            }
            connectedNodes.add(nodeInfoString);
        }
    }

    // Call this method when a node disconnects
    private void removeNode(String nodeInfo) {
        synchronized (connectedNodes) {
            connectedNodes.remove(nodeInfo);
        }
    }

    // Constructor
    public NodeDiscovery(Node localNode) throws SocketException {
        this.localNode = localNode;
        this.socket = new DatagramSocket(0);
        this.discoverySocket = new DatagramSocket(BROADCAST_PORT);
        localNode.setPort(socket.getLocalPort());
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    // Setters for callbacks
    public void setOnNodeDiscoveredCallback(Consumer<String> callback) {
        this.onNodeDiscoveredCallback = callback;
    }

    public void setOnFileBroadcastCallback(Consumer<String> callback) {
        this.onFileBroadcastCallback = callback;
    }

    public void setOnDisconnectCallback(Consumer<String> callback) {
        this.onDisconnectCallback = callback;
    }

    public void setOnDeleteCallback(Consumer<String> callback) {
        this.onDeleteCallback = callback;
    }

    // Send discovery packets to the network
    public void sendDiscoveryPackets() {
        executorService.scheduleAtFixedRate(() -> {
            try {
                byte[] buf;
                String discoveryMessage = "DISCOVERY:" + localNode.getIpAddress() + ":" + localNode.getPort() + ":" + localNode.getSharedSecret();
                buf = discoveryMessage.getBytes();
    
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress, BROADCAST_PORT);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS); // Send discovery every 5 seconds
    }

    // Listen for incoming packets
    public void listenPackets() {
        // Thread for handling discovery and disconnection messages
        new Thread(() -> {
            try {
                byte[] discoveryBuf = new byte[BUFFER_SIZE];
                DatagramPacket discoveryPacket = new DatagramPacket(discoveryBuf, discoveryBuf.length);
                while (true) {
                    discoverySocket.receive(discoveryPacket);
                    String discoveryReceived = new String(discoveryPacket.getData(), 0, discoveryPacket.getLength());

                    // Received a discovery message
                    if (discoveryReceived.startsWith("DISCOVERY:")) { 
                        handleDiscovery(discoveryReceived, discoveryPacket.getAddress().getHostAddress());
                    } 
                    // Received a discovery response
                    else if (discoveryReceived.startsWith("DISCOVERY_RESPONSE:")) {
                        handleDiscoveryResponse(discoveryReceived, discoveryPacket.getAddress().getHostAddress());
                    } 
                    // Received a disconnect message
                    else if (discoveryReceived.startsWith("DISCONNECT:")) {
                        handleDisconnect(discoveryReceived);
                    }

                    try {
                        Thread.sleep(1000); // Sleep for 1 second
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    
        // Thread for handling file operation messages
        new Thread(() -> {
            try {
                byte[] buf = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                while (true) {
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());

                    // Received a file broadcast message
                    if (received.startsWith("FILE:")) {
                        handleFileBroadcast(received);
                    } 
                    // Received a delete message
                    else if (received.startsWith("DELETE:")) {
                        handleDelete(received);
                    }
                    // Received a chunk request 
                    else {
                        handleChunkRequest(received, packet.getAddress(), packet.getPort());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    // Method to handle discovery request
    private void handleDiscovery(String message, String senderIP) {
        synchronized (recentlyDisconnectedNodes) {
            try {
                String[] parts = message.split(":");
                String nodeInfo = parts[1] + ":" + parts[2]; // IP:Port format
                if (recentlyDisconnectedNodes.contains(nodeInfo)) {
                    return; // Ignore JOINED message from recently disconnected node
                }

                // If message is a valid discovery request and the shared secret matches, send a response
                if (parts.length == 4 && parts[3].equals(localNode.getSharedSecret())) {
                    // Send a response back to the sender
                    String responseMessage = "DISCOVERY_RESPONSE:" + localNode.getIpAddress() + ":" + localNode.getPort() + ":" + localNode.getSharedSecret();
                    byte[] responseBuf = responseMessage.getBytes();
                    InetAddress address = InetAddress.getByName(senderIP);
                    DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length, address, BROADCAST_PORT);
                    socket.send(responsePacket);

                    // Add the node to the connected nodes set
                    addNode(nodeInfo);

                    // Trigger the callback
                    if (onNodeDiscoveredCallback != null) {
                        onNodeDiscoveredCallback.accept(nodeInfo);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Method to handle discovery response
    private void handleDiscoveryResponse(String message, String senderIP) {
        synchronized (recentlyDisconnectedNodes) {
            String[] parts = message.split(":");
            String nodeInfo = parts[1] + ":" + parts[2]; // IP:Port format
            if (recentlyDisconnectedNodes.contains(nodeInfo)) {
                return; // Ignore JOINED message from recently disconnected node
            }

            if (parts.length == 4 && parts[3].equals(localNode.getSharedSecret())) {
                // Send a response back to the sender
                sendDiscoveryResponse(senderIP, BROADCAST_PORT);

                // Trigger the callback
                if (onNodeDiscoveredCallback != null) {
                    onNodeDiscoveredCallback.accept(nodeInfo);
                }
            }
        }
    }

    // Method to handle disconnect message
    private void handleDisconnect(String message) {
        String[] parts = message.split(":");
        if (parts.length == 4 && parts[3].equals(localNode.getSharedSecret())) {
            String nodeInfo = parts[1] + ":" + parts[2]; // IP:Port format
            synchronized (recentlyDisconnectedNodes) {
                recentlyDisconnectedNodes.add(nodeInfo);
                //remove the node from this set after a certain timeout
                executorService.schedule(() -> recentlyDisconnectedNodes.remove(nodeInfo), 2, TimeUnit.SECONDS);

                // Remove the node from the connected nodes set
                removeNode(nodeInfo);
            }

            if (onDisconnectCallback != null) {
                onDisconnectCallback.accept(nodeInfo);
            }
        }
    }

    // Method to handle file broadcast
    private void handleFileBroadcast(String message) {
        String[] parts = message.split(":");
        if (parts.length >= 5 && parts[0].equals("FILE") && parts[1].equals(localNode.getSharedSecret())) {
            String fileHash = parts[6];
            String ipAddress = parts[4];
            String fileName = parts[2];
            String fileSize = parts[3];
            int port = Integer.parseInt(parts[5]);

            // Create a peer object
            Peer peer = new Peer(ipAddress, port);

            // Add the file to the map
            FileMetadata metadata = new FileMetadata(fileName, Long.parseLong(fileSize), ipAddress, port, fileHash);
            fileHashMap.put(fileHash, metadata);

            // Add the peer to the list of peers for this file
            filePeersMap.computeIfAbsent(fileHash, k -> new ArrayList<>()).add(peer);

            if (onFileBroadcastCallback != null && !ipAddress.equals(localNode.getIpAddress())) {
                onFileBroadcastCallback.accept(message);
            }
        }
    }

    // Method to handle delete message
    private void handleDelete(String message) {
        String[] parts = message.split(":");
        if (parts.length == 2 && parts[0].equals("DELETE")) {
            String fileHash = parts[1];
            filePeersMap.remove(fileHash);
            fileHashMap.remove(fileHash);

            if (onDeleteCallback != null) {
                onDeleteCallback.accept(fileHash);
            }
        }
    }

    // Method to handle chunk request
    private void handleChunkRequest(String message, InetAddress address, int port) {
        System.out.println("Received chunk request: " + message); // Uncomment for debugging
        String[] parts = message.split(":");
        if (parts.length >= 3 && parts[0].equals("REQUEST_CHUNK")) {
            String fileHash = parts[1];
            int chunkIndex = Integer.parseInt(parts[2]);
    
            // Find and send the requested chunk
            sendChunk(fileHash, chunkIndex, address, port);
        }
    }

    // Method to send a discovery response
    private void sendDiscoveryResponse(String ipAddress, int port) {
        try {
            byte[] responseBuf;
            String responseMessage = "DISCOVERY_RESPONSE:" + localNode.getIpAddress() + ":" + localNode.getPort() + ":" + localNode.getSharedSecret();
            responseBuf = responseMessage.getBytes();
    
            // Send the response to the discovered node
            InetAddress address = InetAddress.getByName(ipAddress);
            DatagramPacket packet = new DatagramPacket(responseBuf, responseBuf.length, address, port);
            discoverySocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to get the local network IP address
    static String getLocalNetworkIP() {
        String ipAddress = null;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                            ipAddress = address.getHostAddress();
                            break;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ipAddress;
    }

    // Broadcast the list of files to the network
    public void broadcastFileList(List<File> fileList) {
        for (File file : fileList) {
            try {
                // Calculate the hash of the file and create a metadata object
                String fileHash = FileMetadata.calculateHash(file);
                FileMetadata metadata = new FileMetadata(file.getName(), file.length(), localNode.getIpAddress(), localNode.getPort(), fileHash);

                String fileBroadcastMessage = "FILE:" + localNode.getSharedSecret() + ":" + metadata.toString();
                byte[] buf = fileBroadcastMessage.getBytes();

                synchronized(connectedNodes) {
                    if (connectedNodes.isEmpty()) {
                        continue;
                    }
        
                    // Broadcast the file to all connected nodes
                    for (String nodeInfo : connectedNodes) {
                        String[] parts = nodeInfo.split(":");
                        if (parts.length != 2) {
                            System.err.println("Invalid node info format: " + nodeInfo); 
                            continue;
                        }

                        String ipAddress = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        InetAddress address = InetAddress.getByName(ipAddress);
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        
                        socket.send(packet);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Broadcast the list of files to the network at a fixed interval
    public void scheduleFileBroadcast(List<File> fileList, long interval, TimeUnit unit) {
        Runnable fileBroadcastTask = () -> broadcastFileList(fileList);
        executorService.scheduleAtFixedRate(fileBroadcastTask, 0, interval, unit);
    }
    
    // Send a chunk request to the network at a fixed interval
    public void scheduleSendDiscoveryPackets(long interval, TimeUnit unit) {
        Runnable discoveryTask = () -> sendDiscoveryPackets();
        executorService.scheduleAtFixedRate(discoveryTask, 0, interval, unit);
    }
    
    // Stop the discovery process
    public void stopDiscovery() {
        executorService.shutdownNow();
        if (socket != null && !socket.isClosed()) {
            socket.close();
            discoverySocket.close();
        }
    }

    // Broadcast a disconnect message to the network
    public void broadcastDisconnect() {
        try {
            String disconnectMessage = "DISCONNECT:" + localNode.getIpAddress() + ":" + localNode.getPort() + ":" + localNode.getSharedSecret();
            byte[] buf = disconnectMessage.getBytes();
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress, BROADCAST_PORT);
            discoverySocket.send(packet);

            // Send files which should be deleted to other nodes
            for (FileMetadata metadata : fileHashMap.values()) {
                String fileHash = metadata.toString().split(":")[4];
                String deleteMessage = "DELETE:" + fileHash;
                buf = deleteMessage.getBytes();

                synchronized(connectedNodes) {
                    if (connectedNodes.isEmpty()) {
                        continue;
                    }

                    // Broadcast the files to all connected nodes
                    for (String nodeInfo : connectedNodes) {
                        String[] parts = nodeInfo.split(":");
                        if (parts.length != 2) {
                            System.err.println("Invalid node info format: " + nodeInfo);
                            continue;
                        }

                        String ipAddress = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        InetAddress address = InetAddress.getByName(ipAddress);
                        packet = new DatagramPacket(buf, buf.length, address, port);
                        socket.send(packet);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to send a chunk to the network
    private void sendChunk(String fileHash, int chunkIndex, InetAddress address, int port) {
        // Find the file with the given hash
        File file = findFileByHash(fileHash, new File(localNode.getSharedFolderPath()));
        if (file != null) {
            // Send the chunk to the requester
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                // Seek to the start of the chunk
                long chunkPosition = (long) chunkIndex * CHUNK_SIZE;
                raf.seek(chunkPosition);

                // Read the chunk data
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead = raf.read(buffer);

                // Send the chunk data to the requester in fragments
                sendFragments(buffer, bytesRead, fileHash, chunkIndex, address, port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Method to send a chunk in fragments
    private void sendFragments(byte[] chunkData, int bytesRead, String fileHash, int chunkIndex, InetAddress address, int port) throws IOException {
        int headerLength = 64 + 4 * 4; // File hash (64 bytes) + 4 integers (4 bytes each)
        int maxDataSizePerFragment = MAX_UDP_PACKET_SIZE - headerLength;
        int totalFragments = (int) Math.ceil((double) bytesRead / maxDataSizePerFragment);
    
        // Send the chunk data in fragments
        for (int i = 0; i < totalFragments; i++) {
            int start = i * maxDataSizePerFragment;
            int end = Math.min(start + maxDataSizePerFragment, bytesRead);
            byte[] fragmentData = Arrays.copyOfRange(chunkData, start, end);
    
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF(fileHash); // fileHash is a string, its length can vary
            dos.writeInt(chunkIndex);
            dos.writeInt(i); // Fragment index
            dos.writeInt(totalFragments); // Total number of fragments
            dos.writeInt(end - start); // Size of this fragment
            dos.write(fragmentData);
    
            byte[] packetData = baos.toByteArray();

            System.out.println("Packet data length: " + packetData.length); // Uncomment for debugging

            if (packetData.length > MAX_UDP_PACKET_SIZE + 10) {
                throw new IOException("Fragment size exceeds maximum UDP packet size");
            }
    
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address, port);
            socket.send(packet);
        }
    }    

    // Method to find a file by its hash
    private File findFileByHash(String fileHash, File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File found = findFileByHash(fileHash, file);
                    if (found != null) {
                        return found;
                    }
                } else {
                    String currentFileHash = FileMetadata.calculateHash(file);
                    if (currentFileHash.equals(fileHash)) {
                        return file;
                    }
                }
            }
        }
        return null;
    }

    // Method to get the list of files
    public List<Peer> getPeersWithFile(String fileHash) {
        return filePeersMap.getOrDefault(fileHash, Collections.emptyList());
    }
}