import os

# Update Common to implement Serializable
def add_serializable(filepath):
    if not os.path.exists(filepath): return
    with open(filepath, 'r') as f:
        content = f.read()
    if 'java.io.Serializable' not in content:
        content = content.replace('package protocol;', 'package protocol;\n\nimport java.io.Serializable;')
        content = content.replace('package protocol.dto;', 'package protocol.dto;\n\nimport java.io.Serializable;')
        content = content.replace('public class ', 'public class ')
        
        # Add implements Serializable
        lines = content.split('\n')
        for i, line in enumerate(lines):
            if 'public class' in line and 'implements Serializable' not in line:
                if '{' in line:
                    lines[i] = line.replace('{', 'implements Serializable {')
                else:
                    lines[i] = line + ' implements Serializable'
        with open(filepath, 'w') as f:
            f.write('\n'.join(lines))

common_dir = "DartGame_Common/src/protocol"
add_serializable(os.path.join(common_dir, "Message.java"))
add_serializable(os.path.join(common_dir, "dto/LoginRequest.java"))
add_serializable(os.path.join(common_dir, "dto/ThrowCoordinateRequest.java"))
add_serializable(os.path.join(common_dir, "dto/ThrowResolvedResponse.java"))

server_files = {
    "DartGame_Server/src/services/Client.java": """package services;

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
""",
    "DartGame_Server/src/controllers/UserController.java": """package controllers;

import protocol.Message;
import protocol.MessageType;
import protocol.dto.LoginRequest;
import services.Client;
import services.ClientManager;

public class UserController {
    public void handleMessage(Client client, Message message) {
        if (message.getType() == MessageType.LOGIN_REQUEST) {
            LoginRequest req = (LoginRequest) message.getPayload();
            // Simplified logic: accept any login and use hash code as ID
            long fakeId = Math.abs(req.getUsername().hashCode());
            client.setLoggedInUserId(fakeId);
            client.setLoggedInUsername(req.getUsername());
            
            ClientManager.getInstance().addClient(client);
            
            client.send(Message.of(MessageType.LOGIN_RESULT, "SUCCESS"));
        } else if (message.getType() == MessageType.LOGOUT_REQUEST) {
            ClientManager.getInstance().removeClient(client.getLoggedInUserId());
            client.setLoggedInUserId(-1);
            client.setLoggedInUsername(null);
            client.send(Message.of(MessageType.LOGOUT_RESULT, "SUCCESS"));
        }
    }
}
""",
    "DartGame_Server/src/controllers/LobbyController.java": """package controllers;

import enums.PlayerStatus;
import protocol.Message;
import protocol.MessageType;
import services.Client;
import services.ClientManager;
import services.RoomManager;
import services.GameService;
import models.Room;

public class LobbyController {
    public void handleMessage(Client client, Message message) {
        if (message.getType() == MessageType.CREATE_BOT_MATCH) {
            Room room = RoomManager.getInstance().createPveRoom(client.getLoggedInUserId());
            ClientManager.getInstance().setStatus(client.getLoggedInUserId(), PlayerStatus.IN_MATCH);
            
            // Send MATCH_STARTED
            client.send(Message.of(MessageType.MATCH_STARTED, room.getMatchId()));
            
            // Start match logic
            GameService.getInstance().startMatch(room);
            
            // Notify turn preparing
            client.send(Message.of(MessageType.TURN_PREPARING, room.getGameState().getTurnId()));
        }
    }
}
""",
    "DartGame_Server/src/controllers/GameController.java": """package controllers;

import protocol.Message;
import protocol.MessageType;
import protocol.dto.ThrowCoordinateRequest;
import protocol.dto.ThrowResolvedResponse;
import services.Client;
import services.GameService;

public class GameController {
    public void handleMessage(Client client, Message message) {
        if (message.getType() == MessageType.ROTATION_SUBMIT) {
            // Assume payload is an Object array [matchId, rotationDegree]
            Object[] payload = (Object[]) message.getPayload();
            String matchId = (String) payload[0];
            int degree = (Integer) payload[1];
            
            GameService.getInstance().submitRotation(matchId, client.getLoggedInUserId(), degree);
            client.send(Message.of(MessageType.ROTATION_APPLIED, degree));
            
        } else if (message.getType() == MessageType.THROW_COORDINATE) {
            ThrowCoordinateRequest req = (ThrowCoordinateRequest) message.getPayload();
            ThrowResolvedResponse res = GameService.getInstance().submitThrow(req, client.getLoggedInUserId());
            
            if (res != null) {
                // Should broadcast to both players, simplified to send to this client
                client.send(Message.of(MessageType.THROW_RESOLVED, res));
            }
        }
    }
}
"""
}

for filepath, content in server_files.items():
    with open(filepath, 'w') as f:
        f.write(content)

print("Controllers and Client Thread implemented.")
