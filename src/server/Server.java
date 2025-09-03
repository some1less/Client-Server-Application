package server;

import common.ConfigReader;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private int serverPort;
    private String serverName;
    private Set<String> bannedWords;
    private ExecutorService service;

    private ConcurrentHashMap<String, ConnectManager> connectedUsers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, ConnectManager> clientPorts = new ConcurrentHashMap<>();

    public Server(String configPath) {
        ConfigReader configReader = new ConfigReader(configPath);
        this.serverPort = configReader.getPort();
        this.serverName = configReader.getServerName();
        this.bannedWords = configReader.getBannedPhrases();
    }

    public void start(){
        Socket clientSocket;

        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {

            System.out.println("[MESSAGE] Waiting for connection...");
            System.out.println("[MESSAGE] Server " + serverName + " started on " + serverPort + " port. :>");
            System.out.println("[MESSAGE] Waiting for clients...");

            while (true) {
                service = Executors.newVirtualThreadPerTaskExecutor();

                clientSocket = serverSocket.accept();
                ConnectManager connectManager = new ConnectManager(this,clientSocket);

                service.submit(connectManager);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Server " + serverName + " error: " + e.getMessage());
        }
    }

    public void addUser(String username, ConnectManager connectManager, int clientPort) {
        connectedUsers.put(username, connectManager);
        clientPorts.put(clientPort, connectManager);
    }

    public void removeUser(String username) {
        connectedUsers.remove(username);
    }

    public ConnectManager getUser(String username) {
        return connectedUsers.get(username);
    }

    public Set<String> getBannedWords() {
        return bannedWords;
    }

    public String getConnectedUsers() {
        return String.join(", ", connectedUsers.keySet());
    }

    public ConcurrentHashMap<String, ConnectManager> getConnectedUsersMap() {
        return connectedUsers;
    }

    public static void main(String[] args) {
        Server server = new Server("config");
        server.start();
    }

}
