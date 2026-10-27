package controllers;
import protocol.*;
import protocol.dto.*;
import protocol.dto.GameMessages.RotationRequest;
import services.*;
public class GameController {
    public void handleMessage(Client client, Message message) {
        long id=client.getLoggedInUserId();
        switch(message.getType()) {
            case ROTATION_SUBMIT -> GameService.getInstance().submitRotation((RotationRequest)message.getPayload(),id);
            case THROW_COORDINATE -> GameService.getInstance().submitThrow((ThrowCoordinateRequest)message.getPayload(),id);
            case LEAVE_MATCH -> GameService.getInstance().leaveMatch((String)message.getPayload(),id,"FORFEIT");
            default -> throw new IllegalArgumentException("Lệnh game không hợp lệ.");
        }
    }
}
