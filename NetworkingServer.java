import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkingServer {
    private static final int PORT = 8888;

    public static void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("SERVER: Market Data Server started on port " + PORT + " (SAR)");

                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                        System.out.println("SERVER: Connection received. Sending adjustment...");
                        out.println("-0.05");
                    } catch (IOException e) {
                        System.err.println("SERVER Error handling client connection: " + e.getMessage());
                    }
                }

            } catch (IOException e) {
                System.err.println("SERVER Error starting server: " + e.getMessage());
            }
        }).start();
    }
}