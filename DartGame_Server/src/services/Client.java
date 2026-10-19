package services;

import protocol.Message;
import protocol.MessageType;
import controllers.UserController;
import controllers.LobbyController;
import controllers.GameController;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client extends Thread {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private long loggedInUserId = -1;
    private String loggedInUsername = null;
    
    private final UserController userController = new UserController();
    private final LobbyController lobbyController = new LobbyController();
    private final GameController gameController = new GameController();

    public Client(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Message message = (Message) in.readObject();
                handleMessage(message);
            }
        } catch (Exception e) {
            System.out.println("Client disconnected.");
        } finally {
            close();
        }
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case LOGIN_REQUEST:
            case REGISTER_REQUEST:
            case LOGOUT_REQUEST:
                userController.handleMessage(this, message);
                break;
            case INVITE_REQUEST:
            case INVITE_ACCEPT:
            case INVITE_REJECT:
            case CREATE_BOT_MATCH:
            case CHAT_MESSAGE:
                lobbyController.handleMessage(this, message);
                break;
            case ROTATION_SUBMIT:
            case THROW_COORDINATE:
            case LEAVE_MATCH:
                gameController.handleMessage(this, message);
                break;
            default:
                break;
        }
    }

    public void send(Message message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (loggedInUserId != -1) {
                ClientManager.getInstance().removeClient(loggedInUserId);
                GameService.getInstance().handleDisconnect("", loggedInUserId); // Simplified
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getLoggedInUserId() { return loggedInUserId; }
    public void setLoggedInUserId(long id) { this.loggedInUserId = id; }
    public String getLoggedInUsername() { return loggedInUsername; }
    public void setLoggedInUsername(String username) { this.loggedInUsername = username; }
}
