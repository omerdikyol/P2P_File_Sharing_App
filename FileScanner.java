import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class FileScanner {
    private String sharedFolderPath;
    private Set<String> excludedFolders;

    public FileScanner(String sharedFolderPath, Set<String> excludedFolders) {
        this.sharedFolderPath = sharedFolderPath;
        this.excludedFolders = excludedFolders != null ? excludedFolders : new HashSet<>();
    }

    // Scan the shared folder for files
    public List<File> scanForFiles() {
        List<File> fileList = new ArrayList<>();
        scanDirectory(new File(sharedFolderPath), fileList);
        return fileList;
    }

    // Recursively scan the shared folder for files
    private void scanDirectory(File folder, List<File> fileList) {
        if (excludedFolders.contains(folder.getAbsolutePath())) {
            return; // Skip the excluded folder
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file);
                } else if (file.isDirectory()) {
                    scanDirectory(file, fileList);
                }
            }
        }
    }
}
