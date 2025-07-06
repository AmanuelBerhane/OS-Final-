import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    private static String clientName = "User";

    public static void main(String[] args) {
        System.out.println("Connecting to chat server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
        
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {
            
            System.out.print("Enter your name: ");
            clientName = scanner.nextLine();
            out.println(clientName);
            
            // Start message receiver thread
            new Thread(new MessageReceiver(in)).start();
            
            System.out.println("Connected! Type '/quit' to exit\n");
            
            // Handle user input
            while (true) {
                String userInput = scanner.nextLine();
                out.println(userInput);
                
                if ("/quit".equalsIgnoreCase(userInput)) {
                    System.out.println("Disconnecting from server...");
                    break;
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + SERVER_ADDRESS);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
    }

    private static class MessageReceiver implements Runnable {
        private final BufferedReader in;

        public MessageReceiver(BufferedReader in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                String serverResponse;
                while ((serverResponse = in.readLine()) != null) {
                    System.out.println(serverResponse);
                }
            } catch (IOException e) {
                System.err.println("Error receiving messages: " + e.getMessage());
            }
        }
    }
}