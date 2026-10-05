package dartgame_server;

import services.Client;
import java.net.ServerSocket;
import java.net.Socket;

public class DartGame_Server {
    public static void main(String[] args) {
        int port = 8080;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("DartGame Server started on port " + port);
            
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());
                
                Client client = new Client(socket);
                client.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
