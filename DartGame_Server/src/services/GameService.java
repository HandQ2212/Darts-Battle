package services;

import enums.ActorType;
import enums.MatchStatus;
import models.GameState;
import models.Room;
import models.ThrowResult;
import protocol.dto.ThrowCoordinateRequest;
import protocol.dto.ThrowResolvedResponse;

public class GameService {
    
    private final DartboardScorer scorer = new DartboardScorer();

    public void startMatch(Room room) {
        // Init GameState
    }

    public void submitRotation(String matchId, long userId, int rotationDegree) {
        // Logic to submit rotation
    }

    public void applyBotRotation(String matchId, int rotationDegree) {
        // Logic for Bot rotation
    }

    public ThrowResolvedResponse submitThrow(ThrowCoordinateRequest request, long userId) {
        // Process throw logic
        return null;
    }

    public void leaveMatch(String matchId, long userId) {
        // Logic when someone leaves
    }

    public void handleDisconnect(String matchId, long userId) {
        // Logic on disconnect
    }

    private void finishMatch(GameState state, Long winnerId, ActorType winnerType, String reason) {
        // Finish logic
    }
}
