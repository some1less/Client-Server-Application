package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ConnectManager implements Runnable {

    private final Server server;
    private Socket clientSocket;
    private String username;
    private int clientPort;

    private boolean isLoggedIn = false;
    private boolean isDisconnected = false;

    private BufferedReader in;
    private PrintWriter out;

    public ConnectManager(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            logIn();

            String message;
            while ((message = in.readLine()) != null) {
                processMessage(message);
            }

        } catch (IOException e) {
            disconnect();
            System.out.println("[SERVER] Client connection failed. " + e.getMessage());
        }
    }

    private void logIn() {
        try {
            while (!isLoggedIn) {
            username = in.readLine();
            if (username == null || username.isEmpty()) {
                out.println("ERROR: Username is null or empty");
                continue;
            }

            synchronized (server) {
                if (server.getConnectedUsers().contains(username)) {
                    out.println("[ERROR]: Username is already in use");

                } else {
                    clientPort = clientSocket.getPort();
                    server.addUser(username, this, clientPort);

                    out.println("[SERVER] CONNECTION ESTABLISHED");

                    System.out.println("[CONNECT MANAGER] " + username + " has connected.");
                    liveMessage(username + " has connected to the chat.", true);

                    out.println("[CONNECTED USERS]" + server.getConnectedUsers());

                    isLoggedIn = true;

                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processMessage(String message) {
        String bannedWord = findBannedWord(message);
        if (bannedWord != null) {
            out.println("[!] Your message contains banned words and it wasn't sent.");
            return;
        } else if(message.equals("/quit")) {
            disconnect();
        } else if (message.startsWith("/whisper ")) {
            String[] splitMessage = message.split(" ", 2);
            if (splitMessage.length >= 2) {

                String[] userAndMessage = splitMessage[1].split(" ", 2);
                if (userAndMessage.length >= 2) {
                    String toUsers = userAndMessage[0];
                    String whisperMessage = userAndMessage[1];
                    String[] users = toUsers.split(",");

                    StringBuilder sentUsers = new StringBuilder();
                    StringBuilder notFoundUsers = new StringBuilder();

                    for (String user : users) {
                        user = user.trim();
                        if (sendMessageToUser(user, "[Whisper from " + username + "]: " + whisperMessage)) {
                            sentUsers.append(user).append(", ");
                        } else {
                            notFoundUsers.append(user).append(", ");
                        }
                    }

                    if (!sentUsers.isEmpty()) {
                        out.println("[Whisper sent to " + sentUsers.substring(0, sentUsers.length() - 2) + "]: " + whisperMessage);
                    }

                    if (!notFoundUsers.isEmpty()) {
                        out.println("[Server]: User(s) not found: " + notFoundUsers.substring(0, notFoundUsers.length() - 2));
                    }

                } else {
                    out.println("[Server]: Incorrect whisper format. Use /whisper username message");
                }
            } else {
                out.println("[Server]: Incorrect whisper format. Use /whisper username message");
            }
        } else if (message.startsWith("/exclude ")) {
            String[] splitMessage = message.split(" ", 2);
            if (splitMessage.length >= 2) {

                String[] userAndMessage = splitMessage[1].split(" ", 2);
                if (userAndMessage.length >= 2) {
                        String excludeUsers = userAndMessage[0];
                        String excludeMessage = userAndMessage[1];

                        String[] users = excludeUsers.split(",");
                        Set<String> excludedUsersSet = new HashSet<>();
                        for (String user : users) {
                            excludedUsersSet.add(user.trim());
                        }

                        liveMessageExcluding(username + ": " + excludeMessage, excludedUsersSet);

                        out.println("[Message sent to all except " + excludeUsers + "]: " + excludeMessage);
                    } else {
                    out.println("[Server]: Incorrect exclude format. Use /exclude username1,username2 message");
                    }
                } else {
                out.println("[Server]: Incorrect exclude format. Use /exclude username1,username2 message");
            }


        } else if (message.startsWith("[BANNED WORDS]")) {
            out.println("[BANNED WORDS]" + server.getBannedWords());
        } else if (message.startsWith("[INSTRUCTIONS]")) {
            out.println("[INSTRUCTIONS] By default, messages are sending to all connected users.");
            out.println("[INSTRUCTIONS] Use /whisper to send private messages, example: /whisper user1 message or /whisper user1,user2 message");
            out.println("[INSTRUCTIONS] Use /exclude to exclude users, example: /exclude user1,user2 message.");
        } else {
            String userMessage = username + ": " + message;
            liveMessage(userMessage, false);
        }
    }

    private void disconnect() {

        if (isDisconnected){
            return;
        }
        isDisconnected = true;

        try {
            if (isLoggedIn) {
                liveMessage(username + " has been disconnected.", true);
                System.out.println("[CONNECT MANAGER] " + username + " has been disconnected.");

                server.removeUser(username);
                isLoggedIn = false;
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    private String findBannedWord(String message) {
        for (String word : server.getBannedWords()) {
            if (message.toLowerCase().contains(word.toLowerCase())) {
                return word;
            }
        }
        return null;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public boolean sendMessageToUser(String username, String message) {
        ConnectManager toUser = server.getUser(username);
        if (toUser != null) {
            toUser.sendMessage(message);
            return true;
        } else {
            return false;
        }
    }

    private void liveMessage(String message, boolean systemMes) {
        Collection<ConnectManager> users = server.getConnectedUsersMap().values();
        for (ConnectManager user : users) {
            user.sendMessage(message);
        }
    }

    private void liveMessageExcluding(String message, Set<String> excludeUsernames) {
        Collection<ConnectManager> users = server.getConnectedUsersMap().values();
        for (ConnectManager user : users) {
            if (!excludeUsernames.contains(user.getUsername())) {
                user.sendMessage(message);
            }
        }
    }

}

