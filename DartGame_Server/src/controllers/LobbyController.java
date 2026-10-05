package controllers;

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
