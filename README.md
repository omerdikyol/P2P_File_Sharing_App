# LAN peer-to-peer file sharing

A Java Swing networking project for discovering peers and sharing files on a local network. Peers advertise file metadata, and downloads request chunks that are split into UDP fragments.

## Run

Install a JDK, then compile and launch from the repository root:

```sh
git clone https://github.com/omerdikyol/P2P_File_Sharing_App.git
cd P2P_File_Sharing_App
javac *.java
java P2PFileSharingApp
```

Run an instance on each machine on the same LAN. In the setup screen, choose a shared folder, exclusions, and the same shared-secret value on each peer. The UI then lists discovered peers/files and transfer progress. Local firewall rules must permit the UDP traffic.

## Code map

- [P2PFileSharingApp](P2PFileSharingApp.java) opens [SetupScreen](SetupScreen.java).
- [NodeDiscovery](NodeDiscovery.java) broadcasts and receives peer/file information.
- [FileScanner](FileScanner.java) builds file metadata.
- [FileDownloader](FileDownloader.java) requests and assembles fragmented chunks.
- [MainScreen](MainScreen.java) displays peers, files, and progress.

## Protocol scope

This is an educational LAN prototype. The shared-secret setting is not evidence of encrypted file transport. Timeout recovery and integrity checking are incomplete: a partial chunk can currently be treated as received. Use disposable sample files when exploring the protocol and verify downloaded contents independently.
