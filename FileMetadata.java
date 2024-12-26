import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class FileMetadata {
    // Instance Variables for FileMetadata
    private String fileName;
    private long fileSize;
    private String ownerIP;
    private int ownerPort;
    private String fileHash;

    public FileMetadata(String fileName, long fileSize, String ownerIP, int ownerPort, String fileHash) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.ownerIP = ownerIP;
        this.ownerPort = ownerPort;
        this.fileHash = fileHash;
    }

    // Hashing Method based on the content of the file - ( Which helps us to identify the files that have the same content but different names )
    public static String calculateHash(File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            byte[] bytesBuffer = new byte[1024];
            int bytesRead = -1;

            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }

            byte[] hashedBytes = digest.digest();

            return convertByteArrayToHexString(hashedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Method to convert the byte array to hex string
    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder stringBuffer = new StringBuilder();
        for (byte arrayByte : arrayBytes) {
            stringBuffer.append(Integer.toString((arrayByte & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuffer.toString();
    }

    // Getters and toString Method
    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getOwnerIP() {
        return ownerIP;
    }

    public int getOwnerPort() {
        return ownerPort;
    }

    @Override
    public String toString() {
        return fileName + ":" + fileSize + ":" + ownerIP + ":" + ownerPort + ":" + fileHash;
    }
}
