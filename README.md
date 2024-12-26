## P2P File Sharing Application
This is a Peer-to-Peer (P2P) file-sharing application that allows users to share files within a local network. The application is built using Java and Swing for the graphical user interface (GUI). It supports file discovery, file transfers, and management of shared and excluded folders.

## Features
- Node Discovery: Automatically discovers other nodes (peers) in the local network.

- File Sharing: Allows users to share files from a specified shared folder.

- Excluded Folders: Users can specify folders to exclude from sharing.

- File Transfers: Supports downloading files from other peers in the network.

- Secure Communication: Uses a secret key for secure communication between nodes.

## Files Overview
1. `MainScreen.java`
The main GUI of the application.

Displays discovered nodes, available files, and ongoing file transfers.

Handles user interactions such as connecting/disconnecting from the network and initiating file downloads.

2. `Node.java`
Represents a node in the P2P network.

Contains information such as IP address, port, shared secret, and shared folder path.

3. `NodeDiscovery.java`
Handles the discovery of nodes in the network.

Manages file broadcasts, disconnections, and communication between nodes.

Uses UDP for broadcasting and receiving messages.

4. `P2PFileSharingApp.java`
The entry point of the application.

Initializes the SetupScreen to start the application.

5. `Peer.java`
Represents a peer in the network.

Contains the IP address and port of the peer.

6. `SetupScreen.java`
The initial setup screen where users configure the application.

Users can specify the secret key, shared folder path, and excluded folders.

## How to Run the Application
1. Compile the Code: Ensure you have Java installed. Compile all the Java files using the following command:

```bash
javac *.java
```
2. Run the Application: Run the P2PFileSharingApp class to start the application:

```bash
java P2PFileSharingApp
```
3. Setup Screen:

Enter a Secret Key for secure communication.

Specify the Shared Folder Path where the files to be shared are located.

Optionally, list any Excluded Folders (one per line) that should not be shared.

4. Main Screen:

Once the setup is complete, the main screen will open.

The application will automatically discover other nodes in the network and display available files.

Users can select files to download, and the progress will be shown in the file transfers table.

## Dependencies
- Java Swing: Used for the graphical user interface.

- Java Networking: Handles communication between nodes using UDP.

## Notes
- Ensure that all nodes in the network use the same Secret Key for secure communication.

- The application is designed for local network use. Ensure that all devices are on the same network for proper functionality.

- The shared folder path should be accessible by the application.

## Troubleshooting
- Connection Issues: Ensure that all devices are on the same network and that firewalls are not blocking UDP traffic.

- File Not Found: Verify that the shared folder path is correct and that the files exist in the specified directory.

- Empty Fields: The secret key and shared folder path cannot be empty. Ensure both fields are filled in the setup screen.

