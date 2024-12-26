import javax.swing.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SetupScreen {
    // Instance variables
    private JFrame frame;
    private JTextField secretKeyField;
    private JTextField sharedFolderPathField;
    private JTextArea excludedFoldersArea;

    public SetupScreen() {
        // Setup screen
        frame = new JFrame("P2P File Sharing Application Setup");
        frame.setSize(400, 270);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        // Secret key
        JLabel secretKeyLabel = new JLabel("Secret Key:");
        secretKeyLabel.setBounds(10, 20, 100, 30);
        frame.add(secretKeyLabel);

        secretKeyField = new JTextField();
        secretKeyField.setBounds(120, 20, 250, 30);
        // secretKeyField.setText("secret"); // Set default secret key // Uncomment for debugging
        frame.add(secretKeyField);

        // Shared folder path
        JLabel sharedFolderPathLabel = new JLabel("Shared Folder Path:");
        sharedFolderPathLabel.setBounds(10, 60, 150, 30);
        frame.add(sharedFolderPathLabel);

        sharedFolderPathField = new JTextField();
        sharedFolderPathField.setBounds(160, 60, 210, 30);
        // sharedFolderPathField.setText("/home/test"); // Set default shared folder path // Uncomment for debugging
        frame.add(sharedFolderPathField);

        // Excluded folders
        JLabel excludedFoldersLabel = new JLabel("Excluded Folders:");
        excludedFoldersLabel.setBounds(10, 100, 150, 30);
        JLabel excludedFoldersLabel2 = new JLabel("(One folder per line)");
        excludedFoldersLabel2.setBounds(10, 120, 150, 30);
        frame.add(excludedFoldersLabel);
        frame.add(excludedFoldersLabel2);

        excludedFoldersArea = new JTextArea();
        excludedFoldersArea.setBounds(160, 100, 210, 80);
        frame.add(excludedFoldersArea);

        // Start button
        JButton startButton = new JButton("Start");
        startButton.setBounds(280, 190, 100, 30);
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String secretKey = secretKeyField.getText();
                String sharedFolderPath = sharedFolderPathField.getText();
                Set<String> excludedFolders = parseExcludedFolders(excludedFoldersArea.getText());

                // If secret key or shared folder path is empty, show error message
                if (secretKey.isEmpty() || sharedFolderPath.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Secret key and shared folder path cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                frame.dispose(); // Close setup screen
                System.out.println("Opening main screen with secret key: " + secretKey + " and shared folder path: " + sharedFolderPath + " and excluded folders: " + excludedFolders); // Uncomment for debugging
                new MainScreen(secretKey, sharedFolderPath, excludedFolders); // Open main screen
            }
        });
        frame.add(startButton);

        frame.setVisible(true);
    }

    // Parse excluded folders from text area
    private Set<String> parseExcludedFolders(String text) {
        Set<String> excludedFolders = new HashSet<>();
        String[] lines = text.split("\n");
        // Add shared folder path to each excluded folder
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                continue;
            }
            lines[i] = sharedFolderPathField.getText() + "/" + lines[i];
        }
        excludedFolders.addAll(Arrays.asList(lines));
        return excludedFolders;
    }
}
