package controllers;

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
