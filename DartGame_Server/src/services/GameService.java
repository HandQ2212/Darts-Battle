package services;

import enums.ActorType;
import enums.MatchStatus;
import models.GameState;
import models.Room;
import models.ThrowResult;
import protocol.Message;
import protocol.MessageType;
import protocol.dto.ThrowCoordinateRequest;
import protocol.dto.ThrowResolvedResponse;

public class GameService {
    private static final GameService instance = new GameService();
    private final DartboardScorer scorer = new DartboardScorer();
    
    private GameService() {}
    public static GameService getInstance() { return instance; }

    public void startMatch(Room room) {
        GameState state = room.getGameState();
        state.setCurrentPlayerId(room.getPlayer1Id());
        state.setCurrentActorType(ActorType.HUMAN);
        state.setStatus(MatchStatus.WAITING_ROTATION);
        
        // Broadcast Match Started
    }

    public synchronized void submitRotation(String matchId, long userId, int rotationDegree) {
        Room room = RoomManager.getInstance().getRoom(matchId);
        if (room == null) return;
        GameState state = room.getGameState();
        
        if (state.getCurrentPlayerId() != null && state.getCurrentPlayerId() == userId) {
            state.setRotationDegree(rotationDegree);
            state.setStatus(MatchStatus.PLAYING);
            // Broadcast ROTATION_APPLIED
        }
    }

    public synchronized ThrowResolvedResponse submitThrow(ThrowCoordinateRequest req, long userId) {
        Room room = RoomManager.getInstance().getRoom(req.getMatchId());
        if (room == null) return null;
        
        GameState state = room.getGameState();
        if (state.getStatus() != MatchStatus.PLAYING) return null;
        
        boolean isPlayer1 = room.getPlayer1Id() == userId;
        int currentRemaining = isPlayer1 ? state.getPlayer1RemainingScore() : state.getPlayer2RemainingScore();
        
        int score = scorer.calculateDartScore(req.getX(), req.getY(), state.getRotationDegree());
        boolean isBust = false;
        
        if (score > currentRemaining) {
            isBust = true;
            // Does not subtract score, but throw counts
        } else {
            currentRemaining -= score;
            if (isPlayer1) state.setPlayer1RemainingScore(currentRemaining);
            else state.setPlayer2RemainingScore(currentRemaining);
        }
        
        state.setDartsThrown(state.getDartsThrown() + 1);
        boolean isTurnFinished = state.getDartsThrown() >= 3 || currentRemaining == 0;
        boolean isMatchFinished = currentRemaining == 0;
        
        ThrowResolvedResponse response = new ThrowResolvedResponse();
        response.setMatchId(req.getMatchId());
        response.setTurnId(state.getTurnId());
        response.setActorType(ActorType.HUMAN); // assuming human for now
        response.setDartIndex(state.getDartsThrown());
        response.setX(req.getX());
        response.setY(req.getY());
        response.setRotationDegree(state.getRotationDegree());
        response.setHitArea(scorer.calculateHitArea(req.getX(), req.getY(), state.getRotationDegree()));
        response.setDartScore(score);
        response.setRemainingScoreAfter(currentRemaining);
        response.setBust(isBust);
        response.setTurnFinished(isTurnFinished);
        response.setMatchFinished(isMatchFinished);
        
        if (isMatchFinished) {
            state.setStatus(MatchStatus.FINISHED);
            finishMatch(room, userId, ActorType.HUMAN, "ZERO_SCORE");
        } else if (isTurnFinished) {
            switchTurn(room);
        }
        
        // In real code, we broadcast this response via ClientManager
        return response;
    }
    
    private void switchTurn(Room room) {
        GameState state = room.getGameState();
        state.setTurnId(state.getTurnId() + 1);
        state.setDartsThrown(0);
        state.setStatus(MatchStatus.WAITING_ROTATION);
        
        if (state.getCurrentPlayerId() != null && state.getCurrentPlayerId() == room.getPlayer1Id()) {
            state.setCurrentPlayerId(room.getPlayer2Id());
            state.setCurrentActorType(room.getPlayer2Type());
            
            if (room.getPlayer2Type() == ActorType.BOT) {
                // Trigger BotService
                BotService.getInstance().playTurn(room.getMatchId());
            }
        } else {
            state.setCurrentPlayerId(room.getPlayer1Id());
            state.setCurrentActorType(ActorType.HUMAN);
        }
    }

    public void finishMatch(Room room, Long winnerId, ActorType winnerType, String reason) {
        RoomManager.getInstance().removeRoom(room.getMatchId());
        if (room.getPlayer1Id() > 0) ClientManager.getInstance().setStatus(room.getPlayer1Id(), PlayerStatus.IDLE);
        if (room.getPlayer2Id() != null) ClientManager.getInstance().setStatus(room.getPlayer2Id(), PlayerStatus.IDLE);
        
        // Broadcast MATCH_FINISHED and call MatchHistoryController
    }
}
