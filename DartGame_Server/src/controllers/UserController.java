package controllers;

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
