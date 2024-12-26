import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FileDownloader implements Runnable {
    private String fileName;
    private String fileHash;
    private long fileSize;
    private String targetFolderPath;
    private List<Peer> peers;
    private MainScreen mainScreen;
    private static final int CHUNK_SIZE = 512 * 1024; // 512 KB
    private static final int MAX_UDP_PACKET_SIZE = 8192;


    public FileDownloader(String fileName, String fileHash, long fileSize, String targetFolderPath, List<Peer> peers, MainScreen mainScreen) {
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.targetFolderPath = targetFolderPath;
        this.peers = peers;
        this.mainScreen = mainScreen;
    }

    // This method is called when the thread is started
    @Override
    public void run() {
        System.out.println("Starting download for " + fileName); // Uncomment for debugging
        System.out.println("Peers: " + peers); // Uncomment for debugging
        int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);
        System.out.println("Total chunks: " + totalChunks); // Uncomment for debugging
        boolean[] chunksReceived = new boolean[totalChunks];
        File outputFile = new File(targetFolderPath, fileName);
        
        
        try (RandomAccessFile file = new RandomAccessFile(outputFile, "rw")) {
            int chunksDownloaded = 0;
            while (chunksDownloaded < totalChunks) {
                for (int i = 0; i < totalChunks; i++) {
                    if (!chunksReceived[i]) {
                        Peer selectedPeer = selectPeerForChunk(i);
                        System.out.println("Selected peer: " + selectedPeer); // Uncomment for debugging
                        if (selectedPeer != null) {
                            byte[] chunkData = receiveAndAssembleChunk(selectedPeer, i);
                            if (chunkData != null) {
                                System.out.println("Received chunk " + i + " from " + selectedPeer); // Uncomment for debugging
                                writeChunkToFile(chunkData, i, file);
                                chunksReceived[i] = true;
                                chunksDownloaded++;
                                updateDownloadProgress(chunksDownloaded, totalChunks);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Select a random peer from the list of peers
    private Peer selectPeerForChunk(int chunkIndex) {
        if (peers.isEmpty()) return null;
        return peers.get(new Random().nextInt(peers.size()));
    }

    // Receive the chunk from the peer and assemble it
    private byte[] receiveAndAssembleChunk(Peer peer, int chunkIndex) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(10000); // Set timeout to 10 seconds
            requestChunkFromPeer(socket, peer, chunkIndex);

            Map<Integer, byte[]> fragments = new HashMap<>();
            int totalFragments = -1;
            boolean receivedAllFragments = false;

            // Keep receiving packets until all fragments are received
            while (!receivedAllFragments) {
                byte[] buffer = new byte[MAX_UDP_PACKET_SIZE + 20];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException e) {
                    System.out.println("Socket timed out while waiting for chunk " + chunkIndex + " from " + peer);
                    break;
                }

                try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()))) {
                    // Read the header of the packet to determine the type of packet
                    String receivedFileHash = dis.readUTF();
                    int receivedChunkIndex = dis.readInt();
                    if (!receivedFileHash.equals(fileHash) || receivedChunkIndex != chunkIndex) {
                        continue; // Skip if not the expected chunk
                    }

                    // Read the fragment data from the packet
                    int fragmentIndex = dis.readInt();
                    totalFragments = dis.readInt();
                    int size = dis.readInt();
                    byte[] fragmentData = new byte[size];
                    dis.readFully(fragmentData);

                    // Store the fragment data in a map
                    fragments.put(fragmentIndex, fragmentData);
                    receivedAllFragments = fragments.size() == totalFragments;
                }
            }

            // Assemble the chunk from the fragments
            return assembleChunk(fragments, totalFragments);
        }
    }

    // Send a request to the peer to send the chunk
    private void requestChunkFromPeer(DatagramSocket socket, Peer peer, int chunkIndex) throws IOException {
        String request = "REQUEST_CHUNK:" + fileHash + ":" + chunkIndex;
        byte[] requestData = request.getBytes();
        InetAddress address = InetAddress.getByName(peer.getIpAddress());
        DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, address, peer.getPort());
        socket.send(requestPacket);
    }

    // Assemble the chunk from the fragments
    private byte[] assembleChunk(Map<Integer, byte[]> fragments, int totalFragments) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < totalFragments; i++) {
            byte[] fragment = fragments.get(i);
            if (fragment != null) {
                baos.write(fragment, 0, fragment.length);
            }
        }
        return baos.toByteArray();
    }

    // Write the chunk data to the file
    private void writeChunkToFile(byte[] chunkData, int chunkIndex, RandomAccessFile file) throws IOException {
        long offset = (long) chunkIndex * CHUNK_SIZE;
        file.seek(offset);
        file.write(chunkData);
    }

    // Update the progress of the download on the main screen
    private void updateDownloadProgress(int chunksDownloaded, int totalChunks) {
        int progressPercentage = (int) (((double) chunksDownloaded / totalChunks) * 100);
        mainScreen.updateDownloadProgress(fileName, progressPercentage);
    }
}
