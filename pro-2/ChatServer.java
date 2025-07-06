import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 8080;
    private static final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static int clientIdCounter = 0;
    private static final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        System.out.println("Starting chat server on port " + PORT + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientId = ++clientIdCounter;
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);
                clients.put(clientId, clientHandler);
                new Thread(clientHandler).start();
                
                logConnection(clientId, "connected");
                broadcastSystemMessage("Client " + clientId + " has joined the chat");
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    public static void broadcastMessage(String message, int senderId) {
        String formattedMessage = String.format("[Client %d] %s", senderId, message);
        
        clients.forEach((id, client) -> {
            if (id != senderId) {
                client.sendMessage(formattedMessage);
            }
        });
        
        logMessage(senderId, message);
    }

    public static void broadcastSystemMessage(String message) {
        clients.forEach((id, client) -> client.sendMessage("[SYSTEM] " + message));
        System.out.println("[SYSTEM] " + message);
    }

    public static void removeClient(int clientId) {
        clients.remove(clientId);
        logConnection(clientId, "disconnected");
        broadcastSystemMessage("Client " + clientId + " has left the chat");
    }

    private static void logConnection(int clientId, String status) {
        String logEntry = String.format("[%s] Client %d %s", 
            timestampFormat.format(new Date()), clientId, status);
        System.out.println(logEntry);
        writeToLogFile(logEntry);
    }

    private static void logMessage(int clientId, String message) {
        String logEntry = String.format("[%s] Client %d: %s", 
            timestampFormat.format(new Date()), clientId, message.trim());
        writeToLogFile(logEntry);
    }

    private static void writeToLogFile(String content) {
        try (FileWriter fw = new FileWriter("server_log.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(content);
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final int clientId;
    private PrintWriter out;

    public ClientHandler(Socket socket, int id) {
        this.clientSocket = socket;
        this.clientId = id;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()))) {
            
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println("[SYSTEM] Connected to server. Your ID: " + clientId);
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if ("/quit".equalsIgnoreCase(inputLine)) {
                    break;
                }
                ChatServer.broadcastMessage(inputLine, clientId);
            }
        } catch (IOException e) {
            System.err.println("Error handling client #" + clientId + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket for client #" + clientId);
            }
            ChatServer.removeClient(clientId);
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}