package client;

import common.PlaceHolder;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client extends JFrame {

    private Socket socket;

    private String username;

    private JButton instructionsButton;
    private JButton bannedWordsButton;
    private JButton disconnectButton;
    private JTextArea messageArea;
    private JTextField inputField;
    private JTextField usernameField;
    private PlaceHolder hostField;
    private PlaceHolder portField;
    private JPanel inputPanel;
    private JPanel topPanel;
    private boolean isConnected = false;

    private BufferedReader in;
    private PrintWriter out;

    public Client() {
        clientGUI();
    }

    public void clientGUI(){
        setTitle("My Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 650);
        setResizable(false);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        JButton connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        bannedWordsButton = new JButton("Banned Words");
        bannedWordsButton.setPreferredSize(new Dimension(120, 25));
        bannedWordsButton.setEnabled(false);
        instructionsButton = new JButton("Instructions");
        instructionsButton.setPreferredSize(new Dimension(120, 25));
        instructionsButton.setEnabled(false);

        usernameField = new JTextField(15);
        hostField = new PlaceHolder("localhost");
        hostField.setColumns(10);
        portField = new PlaceHolder("port");
        portField.setColumns(5);

        inputField = new JTextField();
        inputField.setPreferredSize(new Dimension(300,50));

        JButton jButton = new JButton("Send");
        jButton.setPreferredSize(new Dimension(100, 35));

        inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(instructionsButton);
        inputPanel.add(bannedWordsButton);
        inputPanel.add(inputField);
        inputPanel.add(jButton);

        inputField.setEnabled(false);
        jButton.setEnabled(false);

        topPanel = new JPanel();
        topPanel.add(hostField);
        topPanel.add(portField);
        topPanel.add(new JLabel("Username:"));
        topPanel.add(usernameField);
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);

        // Add components to the frame
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        instructionsButton.addActionListener(e -> openInstructions());
        bannedWordsButton.addActionListener(e -> openBannedWords());
        connectButton.addActionListener(e -> login());
        jButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        disconnectButton.addActionListener(e -> disconnect());
    }

    private void login(){
        username = usernameField.getText();
        String host = hostField.getText();
        try {
            int port = Integer.parseInt(portField.getText().trim());

            if (!username.isEmpty() && !host.isEmpty() && port > 0 && port < 65536) {
                connect(host, port);
            } else {
                JOptionPane.showMessageDialog(this, "Username OR Ip-address cannot be empty.");
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port cannot be null or text format.");
        }
    }

    private void sendMessage() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(this, "You must log in first.");
            return;
        }

        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            inputField.setText("");
        }
    }

    private void connect(String host, int port){
        Thread.ofVirtual().start(() -> {
            try {
                socket = new Socket(host, port);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println(username);
                isConnected = true;

                serverResponse();

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Connection error: Could not connect to " + host + ":" + port + ". Try again.");
                    isConnected = false;
                });
            }
        });
    }

    private void serverResponse(){
        try {

            username = usernameField.getText();
            String host = hostField.getText();
            int port = Integer.parseInt(portField.getText().trim());

            String response;
            while ((response = in.readLine()) != null) {
                String finalResponse = response;

                if(!response.startsWith("[BANNED WORDS]")
                        && !response.startsWith("/quit")
                        && !response.startsWith("[CONNECTED USERS]")
                        && !response.startsWith("[INSTRUCTIONS]")) {
                    SwingUtilities.invokeLater(() -> messageArea.append(finalResponse + "\n"));
                }
                if (response.startsWith("[BANNED WORDS]")) {
                    JOptionPane.showMessageDialog(this, finalResponse.substring("[BANNED WORDS]".length()));
                } else if(response.startsWith("[CONNECTED USERS]")) {
                    JOptionPane.showMessageDialog(this, "Current connected users: " + response.substring("[CONNECTED USERS]".length()));
                } else if (response.startsWith("[INSTRUCTIONS]")) {
                    JOptionPane.showMessageDialog(this, finalResponse.substring("[INSTRUCTIONS]".length()));
                } else if (response.equals("[SERVER] CONNECTION ESTABLISHED")) {

                    SwingUtilities.invokeLater(() -> {
                        setTitle("My Client - " + username);
                        for (Component component : topPanel.getComponents()) {
                            component.setEnabled(false);
                        }

                        disconnectButton.setEnabled(true);
                        bannedWordsButton.setEnabled(true);

                        inputPanel.setEnabled(true);
                        for (Component component : inputPanel.getComponents()) {
                            component.setEnabled(true);
                        }

                    });
                }

            }
        } catch (IOException e){
            System.exit(0);
        }
    }

    private void disconnect() {
        try {
            if (out != null) {
                out.println("/quit"); // Inform the server about disconnection
                out.close();
            }
            if (in != null) in.close();
            if (socket != null) socket.close();

        } catch (IOException e) {
            System.exit(0);
        }
    }

    private void directMessage(String message) {
        out.println("/whisper " + username + message);
    }

    private void openBannedWords(){
        out.println("[BANNED WORDS]");
    }

    private void openInstructions(){
        out.println("[INSTRUCTIONS]");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
            JOptionPane.showMessageDialog(client,"Welcome to My Client! :> " +
                    "\n" + "This is program for chatting between users." +
                    "\n" + "On the top, you have to provide info about server ip, server port and your name.");
        });
    }
}
