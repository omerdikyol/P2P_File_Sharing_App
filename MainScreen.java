import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainScreen {
    // Instance Variables for MainScreen
    private JFrame frame;
    private JList<String> nodeList;
    private JList<String> fileList;
    private NodeDiscovery nodeDiscovery;
    private String sharedFolderPath;
    private String secretKey;
    private Set<String> excludedFolders;

    // Models for JLists and JTable
    private DefaultListModel<String> nodeListModel;
    private DefaultListModel<String> fileListModel;
    private DefaultTableModel fileTransfersModel;
    private JTable fileTransfersTable;
    private JLabel hostnameLabel;
    private JLabel ipLabel;

    // Map to store file metadata 
    private Map<String, String> fileMetadataMap = new HashMap<>();

    // Menu Items
    private JMenuItem connectItem;
    private JMenuItem disconnectItem;

    public MainScreen(String secretKey, String sharedFolderPath, Set<String> excludedFolders) {
        this.secretKey = secretKey;
        this.sharedFolderPath = sharedFolderPath;
        this.excludedFolders = excludedFolders != null ? excludedFolders : new HashSet<>();

        // Initialize the UI
        frame = new JFrame("P2P File Sharing Application");
        frame.setSize(600, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Top panel for Node and File Lists
        JPanel topPanel = new JPanel(new GridLayout(1, 2));

        // Node and File Lists
        nodeList = new JList<>();
        fileList = new JList<>();

        // Node List
        JPanel nodePanel = new JPanel(new BorderLayout());
        JLabel nodeLabel = new JLabel("Computers in Network:");
        nodePanel.add(nodeLabel, BorderLayout.NORTH);
        nodePanel.add(new JScrollPane(nodeList), BorderLayout.CENTER);
        topPanel.add(nodePanel);
        
        nodeListModel = new DefaultListModel<>();
        nodeList.setModel(nodeListModel); // Set the model after initializing nodeList

        // File List
        JPanel filePanel = new JPanel(new BorderLayout());
        JLabel fileLabel = new JLabel("Files Found:");
        filePanel.add(fileLabel, BorderLayout.NORTH);
        filePanel.add(new JScrollPane(fileList), BorderLayout.CENTER);
        topPanel.add(filePanel);
        
        fileListModel = new DefaultListModel<>();
        fileList.setModel(fileListModel);

        // Add top panel to the frame
        frame.add(topPanel, BorderLayout.CENTER);

        // Bottom panel for File Transfers Table, hostname and IP
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // File Transfers Table
        JPanel fileTransfersPanel = new JPanel(new BorderLayout());
        JLabel fileTransfersLabel = new JLabel("File Transfers:");
        fileTransfersPanel.add(fileTransfersLabel, BorderLayout.NORTH);
        fileTransfersModel = new DefaultTableModel(new Object[]{"File Name", "Progress", "Status"}, 0);
        fileTransfersTable = new JTable(fileTransfersModel);
        fileTransfersPanel.add(new JScrollPane(fileTransfersTable), BorderLayout.CENTER);        
        
        // Hostname and IP Labels
        hostnameLabel = new JLabel();
        ipLabel = new JLabel();
        
        bottomPanel.add(hostnameLabel, BorderLayout.WEST);
        bottomPanel.add(ipLabel, BorderLayout.EAST);
        bottomPanel.add(fileTransfersPanel, BorderLayout.NORTH);

        // Add bottom panel to the frame
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Set hostname and IP
        setHostnameAndIP();

        // Menu
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        // Files Menu
        JMenu fileMenu = new JMenu("Files");
        menuBar.add(fileMenu);
        connectItem = new JMenuItem("Connect");
        disconnectItem = new JMenuItem("Disconnect");
        JMenuItem exitItem = new JMenuItem("Exit");

        // Disable disconnect menu item
        disconnectItem.setEnabled(false);

        fileMenu.add(connectItem);
        fileMenu.add(disconnectItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        JMenuItem aboutItem = new JMenuItem("About");

        helpMenu.add(aboutItem);

        // Event Listeners
        connectItem.addActionListener(e -> connect());
        disconnectItem.addActionListener(e -> disconnect());
        exitItem.addActionListener(e -> exit());
        aboutItem.addActionListener(e -> showAboutInfo());

        fileList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onFileSelected();
                }
            }

            @Override public void mousePressed(java.awt.event.MouseEvent e) {}
            @Override public void mouseReleased(java.awt.event.MouseEvent e) {}
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {}
            @Override public void mouseExited(java.awt.event.MouseEvent e) {}
        });

        frame.setVisible(true);
    }

    private void startFileDownload(String fileName, String fileHash, long fileSize) {
        // Retrieve the list of peers having the file
        List<Peer> peersWithFile = getPeersWithFile(fileHash);

        System.out.println("Peers with file: " + peersWithFile); // Uncomment for debugging
    
        // Start the FileDownloader in a new thread
        FileDownloader downloader = new FileDownloader(fileName, fileHash, fileSize, sharedFolderPath, peersWithFile, this);
        new Thread(downloader).start();

        // Add entry to file transfers table
        fileTransfersModel.addRow(new Object[]{fileName, "0%", "Downloading"});
    }

    private void processFileInformation(String fileInfo) {
        // Extract additional details from fileInfo
        String[] parts = fileInfo.split(":");
        String fileName = parts[2];
        String fileHash = parts[6];

        // Combine file name and hash to create a unique key (in case of duplicate file names)
        String uniqueKey = fileName + ":" + fileHash;
        
        // Store the full metadata using the unique key
        fileMetadataMap.put(uniqueKey, fileInfo);

        // Add unique key to the file list model
        if (!fileListModel.contains(uniqueKey)) {
            fileListModel.addElement(uniqueKey);
        }
    }

    private void onFileSelected() {
        // Get the selected file's unique key
        String selectedUniqueKey = fileList.getSelectedValue();
        // Check if the file info is available
        if (selectedUniqueKey != null && fileMetadataMap.containsKey(selectedUniqueKey)) {
            String fullMetadata = fileMetadataMap.get(selectedUniqueKey);
            // Extract additional details from fullMetadata
            String[] parts = fullMetadata.split(":");
            String fileName = parts[2];
            String fileHash = parts[6];
            long fileSize = Long.parseLong(parts[3]);

            startFileDownload(fileName, fileHash, fileSize);
        } else { // This should never happen
            System.out.println("File info not found for selected file: " + selectedUniqueKey);
        }
    }

    // Connect to the network
    private void connect() {
        try {
            String ipAddress = NodeDiscovery.getLocalNetworkIP();

            // Create a local node and start discovery
            Node localNode = new Node(ipAddress, 0, secretKey, sharedFolderPath);
            nodeDiscovery = new NodeDiscovery(localNode);

            // Start File Scanner and Broadcast
            FileScanner fileScanner = new FileScanner(sharedFolderPath, excludedFolders);
            List<File> fileList = fileScanner.scanForFiles();
            nodeDiscovery.scheduleSendDiscoveryPackets(5, TimeUnit.SECONDS);
            nodeDiscovery.scheduleFileBroadcast(fileList, 6, TimeUnit.SECONDS);

            // Start listening for packets
            nodeDiscovery.listenPackets();

            // Set callbacks (what to do when a node is discovered, file is broadcasted, etc.)

            // Discover nodes
            nodeDiscovery.setOnNodeDiscoveredCallback(nodeInfo -> {
                SwingUtilities.invokeLater(() -> {
                    if (!nodeListModel.contains(nodeInfo)) {
                        nodeListModel.addElement(nodeInfo);
                    }
                });
            });

            // Broadcast files
            nodeDiscovery.setOnFileBroadcastCallback(fileInfo -> {
                SwingUtilities.invokeLater(() -> {
                    if (!fileListModel.contains(fileInfo)) {
                        processFileInformation(fileInfo);
                    }
                });
            });

            // Remove nodes
            nodeDiscovery.setOnDisconnectCallback(nodeInfo -> {
                SwingUtilities.invokeLater(() -> {
                    removeNodeFromList(nodeInfo);
                });
            });

            // Remove files
            nodeDiscovery.setOnDeleteCallback(fileHash -> {
                SwingUtilities.invokeLater(() -> {
                    // Remove the file from the file list
                    for (int i = fileListModel.getSize() - 1; i >= 0; i--) {
                        String fileInfo = fileListModel.getElementAt(i);
                        // Get full metadata from fileMetadataMap
                        fileInfo = fileMetadataMap.get(fileInfo);
                        String[] parts = fileInfo.split(":");
                        String hash = parts[6];
                        if (hash.equals(fileHash)) {
                            fileListModel.removeElementAt(i);
                        }
                    }
                });
            });
            
            // Disable connect menu item
            connectItem.setEnabled(false);
            // Enable disconnect menu item
            disconnectItem.setEnabled(true);
        

            JOptionPane.showMessageDialog(frame, "Connected to the network.",
                    "Connection", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Failed to connect: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Disconnect from the network
    private void disconnect() {
        if (nodeDiscovery != null) {
            // Stop broadcasting and listening
            nodeDiscovery.broadcastDisconnect();
            nodeDiscovery.stopDiscovery();
            JOptionPane.showMessageDialog(frame, "Disconnected from the network.",
            "Disconnection", JOptionPane.INFORMATION_MESSAGE);

            // Clear the GUI
            fileListModel.clear();
            nodeListModel.clear();
            fileMetadataMap.clear();
            fileTransfersModel.setRowCount(0);
            // Enable connect menu item
            connectItem.setEnabled(true);
            // Disable disconnect menu item
            disconnectItem.setEnabled(false);
        }
    }

    // Exit the application
    private void exit() {
        disconnect();
        System.exit(0);
    }

    // Remove a node from the list at the GUI
    private void removeNodeFromList(String nodeInfo) {
        for (int i = 0; i < nodeListModel.getSize(); i++) {
            if (nodeListModel.get(i).equals(nodeInfo)) {
                nodeListModel.remove(i);
                break;
            }
        }
    }

    // Show about information
    private void showAboutInfo() {
        JOptionPane.showMessageDialog(frame,
            "P2P File Sharing Application\nDeveloper: Ömer Dikyol\nStudent Number: 20200702002",
            "About", JOptionPane.INFORMATION_MESSAGE);
    }

    // Update the download progress for a file
    public void updateDownloadProgress(String fileName, int progress) {
        SwingUtilities.invokeLater(() -> {
            // Find the row for the file and update its progress
            for (int i = 0; i < fileTransfersModel.getRowCount(); i++) {
                if (fileTransfersModel.getValueAt(i, 0).equals(fileName)) {
                    fileTransfersModel.setValueAt(progress + "%", i, 1); // Assuming column 1 is for progress
                    if (progress == 100) {
                        fileTransfersModel.setValueAt("Completed", i, 2); // Assuming column 2 is for status
                    }
                    return;
                }
            }
    
            // If the file is not in the table, add it
            fileTransfersModel.addRow(new Object[]{fileName, progress + "%", "Downloading..."});
        });
    }

    // Set the hostname and IP labels at the bottom panel
    private void setHostnameAndIP() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            hostnameLabel.setText(" Hostname: " + localHost.getHostName());
            ipLabel.setText("IP: " + getLocalNetworkIP() + " ");
        } catch (UnknownHostException e) {
            hostnameLabel.setText("Hostname: Unknown");
            ipLabel.setText("IP: Unknown");
            e.printStackTrace();
        }
    }

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

    // Get the list of peers having the file
    private List<Peer> getPeersWithFile(String fileHash) {
        return nodeDiscovery.getPeersWithFile(fileHash);
    }
}