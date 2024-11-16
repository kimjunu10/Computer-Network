import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;

public class QuizClient {
    // Declare GUI components
    private static JFrame frame; // Main application window
    private static JTextArea displayArea; // Area to display messages
    private static JTextField answerField; // Input field for user answers
    private static JButton sendButton; // Button to send answers
    private static BufferedReader in; 
    private static BufferedWriter out; 

    public static void main(String[] args) {
        // Create and display the GUI in a separate thread
        SwingUtilities.invokeLater(QuizClient::createAndShowGUI);

        // Default server IP and port configuration
        String serverIP = "localhost"; // Default server IP
        int port = 1234;               // Default port

        try {
            // Attempt to read server configuration from a file
            BufferedReader reader = new BufferedReader(new FileReader("server_info.dat"));
            serverIP = reader.readLine().trim();
            port = Integer.parseInt(reader.readLine().trim());
            reader.close();
        } catch (IOException e) {
            // Handle missing or unreadable configuration file
            showMessage("Configuration file 'server_info.dat' not found or unreadable.\n");
            showMessage("Using default server IP: " + serverIP + " and port: " + port + "\n");
        }

        try {
            // Attempt to connect to the server
            Socket clientSocket = new Socket(serverIP, port);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            // Start a separate thread to handle incoming messages
            new Thread(QuizClient::receiveMessages).start();
        } catch (IOException e) {
            // Display error message and close the client on failure
            showMessage("Unable to connect to server at " + serverIP + ":" + port + ".\n");
            closeClient();
        }
    }

    private static void createAndShowGUI() {
        // Set up and display the GUI
        frame = new JFrame("Quiz Client"); // Create the main application window
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400); // Set the size of the window
        frame.setLocationRelativeTo(null); // Center the window on the screen

        displayArea = new JTextArea(); // Create the message display area
        displayArea.setEditable(false); // Make it read-only
        displayArea.setLineWrap(true); // Enable line wrapping
        displayArea.setWrapStyleWord(true); // Wrap lines at word boundaries

        JScrollPane scrollPane = new JScrollPane(displayArea); // Add scroll functionality

        answerField = new JTextField(20); // Create the input field
        answerField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Send the answer when the Enter key is pressed
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendAnswer();
                }
            }
        });

        sendButton = new JButton("Send"); // Create the send button
        sendButton.addActionListener(e -> sendAnswer()); // Attach an action listener to the button

        JPanel inputPanel = new JPanel(new BorderLayout()); // Create the input panel
        inputPanel.add(answerField, BorderLayout.CENTER); // Add the input field to the panel
        inputPanel.add(sendButton, BorderLayout.EAST); // Add the send button to the panel

        // Add components to the main window
        frame.add(scrollPane, BorderLayout.CENTER); // Add the message display area
        frame.add(inputPanel, BorderLayout.SOUTH); // Add the input panel

        frame.setVisible(true); // Display the main window
    }

    private static void receiveMessages() {
        try {
            // Continuously read messages from the server
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                // Process messages based on their type
                if (serverMessage.startsWith("TYPE:")) {
                    String type = serverMessage.substring(5); // Extract the message type
                    String message = in.readLine(); // Read the message content

                    switch (type) {
                        case "WELCOME" -> showMessage(message + "\n\n"); // Display a welcome message
                        case "QUESTION" -> {
                            // Display the question and score
                            String question = message;
                            String scoreLine = in.readLine(); // Read the score line
                            showMessage(question + " " + scoreLine + "\n");
                        }
                        case "FEEDBACK" -> showMessage(message + "\n\n"); // Display feedback
                        case "SCORE" -> {
                            // Display the final score and disable input
                            showMessage(message + "\n");
                            disableInput();
                        }
                        case "HINT" -> showMessage(message + "\n"); // Display a hint
                    }
                }
            }
        } catch (IOException e) {
            // Handle connection loss
            showMessage("Connection lost.\n");
            closeClient();
        }
    }

    private static void sendAnswer() {
        // Send the user's answer to the server
        String answer = answerField.getText().trim();
        if (!answer.isEmpty() && out != null) {
            showMessage("Your answer: " + answer + "\n"); 
            try {
                out.write(answer + "\n"); // Send the answer to the server
                out.flush(); // Ensure the message is sent
            } catch (IOException e) {
                showMessage("Error sending answer.\n"); 
            }
            answerField.setText(""); // Clear the input field
        }
    }

    private static void showMessage(String message) {
        // Append a message to the display area (execute in the GUI thread)
        SwingUtilities.invokeLater(() -> displayArea.append(message));
    }

    private static void disableInput() {
        // Disable the input field and send button (execute in the GUI thread)
        SwingUtilities.invokeLater(() -> {
            answerField.setEditable(false);
            sendButton.setEnabled(false);
        });
    }

    private static void closeClient() {
        // Handle client closure
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame, "Connection to the server has been lost.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            frame.dispose(); // Close the main window
        });
    }
}
