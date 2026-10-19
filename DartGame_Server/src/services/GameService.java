package services;

import controllers.MatchHistoryController;
import enums.ActorType;
import enums.MatchStatus;
import enums.PlayerStatus;
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
    private final MatchHistoryController historyController = new MatchHistoryController();
    
    private GameService() {}
    public static GameService getInstance() { return instance; }

    public void startMatch(Room room) {
        GameState state = room.getGameState();
        state.setCurrentPlayerId(room.getPlayer1Id());
        state.setCurrentActorType(ActorType.HUMAN);
        state.setStatus(MatchStatus.WAITING_ROTATION);
        
        // Save to DB
        historyController.createMatch(room.getMatchId(), room.getMode().name(), room.getPlayer1Id(), room.getPlayer2Id(), room.getPlayer2Type().name());
        
        // Broadcast Match Started
        ClientManager.getInstance().sendToUser(room.getPlayer1Id(), Message.of(MessageType.MATCH_STARTED, room.getMatchId()));
        if (room.getPlayer2Id() != null) {
            ClientManager.getInstance().sendToUser(room.getPlayer2Id(), Message.of(MessageType.MATCH_STARTED, room.getMatchId()));
        }
    }

    public synchronized void submitRotation(String matchId, long userId, int rotationDegree) {
        Room room = RoomManager.getInstance().getRoom(matchId);
        if (room == null) return;
        GameState state = room.getGameState();
        
        if (state.getCurrentPlayerId() != null && state.getCurrentPlayerId() == userId) {
            state.setRotationDegree(rotationDegree);
            state.setStatus(MatchStatus.PLAYING);
            // Broadcast ROTATION_APPLIED
            broadcast(room, Message.of(MessageType.ROTATION_APPLIED, rotationDegree));
            broadcast(room, Message.of(MessageType.TURN_STARTED, null));
        }
    }
    
    public synchronized void applyBotRotation(String matchId, int rotationDegree) {
        Room room = RoomManager.getInstance().getRoom(matchId);
        if (room == null) return;
        GameState state = room.getGameState();
        if (state.getCurrentActorType() == ActorType.BOT) {
            state.setRotationDegree(rotationDegree);
            state.setStatus(MatchStatus.PLAYING);
            broadcast(room, Message.of(MessageType.ROTATION_APPLIED, rotationDegree));
            broadcast(room, Message.of(MessageType.TURN_STARTED, null));
        }
    }

    public synchronized ThrowResolvedResponse submitThrow(ThrowCoordinateRequest req, Long userId) {
        Room room = RoomManager.getInstance().getRoom(req.getMatchId());
        if (room == null) return null;
        
        GameState state = room.getGameState();
        if (state.getStatus() != MatchStatus.PLAYING) return null;
        
        boolean isPlayer1 = room.getPlayer1Id() == (userId != null ? userId : -1);
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
        
        ThrowResult result = new ThrowResult();
        result.setMatchId(req.getMatchId());
        result.setTurnId(state.getTurnId());
        result.setActorName(isPlayer1 ? "Player1" : (userId != null ? "Player2" : "BOT"));
        result.setActorType(state.getCurrentActorType());
        result.setDartIndex(state.getDartsThrown());
        result.setX(req.getX());
        result.setY(req.getY());
        result.setRotationDegree(state.getRotationDegree());
        result.setHitArea(scorer.calculateHitArea(req.getX(), req.getY(), state.getRotationDegree()));
        result.setDartScore(score);
        result.setRemainingScoreAfter(currentRemaining);
        result.setBust(isBust);
        result.setTurnFinished(isTurnFinished);
        result.setMatchFinished(isMatchFinished);
        
        historyController.saveThrow(result, userId);
        
        ThrowResolvedResponse response = new ThrowResolvedResponse();
        response.setMatchId(result.getMatchId());
        response.setTurnId(result.getTurnId());
        response.setActorName(result.getActorName());
        response.setActorType(result.getActorType());
        response.setDartIndex(result.getDartIndex());
        response.setX(result.getX());
        response.setY(result.getY());
        response.setRotationDegree(result.getRotationDegree());
        response.setHitArea(result.getHitArea());
        response.setDartScore(result.getDartScore());
        response.setRemainingScoreAfter(result.getRemainingScoreAfter());
        response.setBust(result.isBust());
        response.setTurnFinished(result.isTurnFinished());
        response.setMatchFinished(result.isMatchFinished());
        
        broadcast(room, Message.of(MessageType.THROW_RESOLVED, response));
        
        if (isMatchFinished) {
            state.setStatus(MatchStatus.FINISHED);
            finishMatch(room, userId, state.getCurrentActorType(), "ZERO_SCORE");
        } else if (isTurnFinished) {
            switchTurn(room);
        }
        
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
                BotService.getInstance().playTurn(room.getMatchId());
            }
        } else {
            state.setCurrentPlayerId(room.getPlayer1Id());
            state.setCurrentActorType(ActorType.HUMAN);
        }
        
        broadcast(room, Message.of(MessageType.TURN_PREPARING, state.getTurnId()));
    }

    public void handleDisconnect(String matchId, long userId) {
        Room room = RoomManager.getInstance().getRoom(matchId);
        if (room == null) return;
        Long opponent = room.getOpponentUserId(userId);
        finishMatch(room, opponent, opponent == null ? ActorType.BOT : ActorType.HUMAN, "DISCONNECT");
    }

    public void leaveMatch(String matchId, long userId) {
        Room room = RoomManager.getInstance().getRoom(matchId);
        if (room == null) return;
        Long opponent = room.getOpponentUserId(userId);
        finishMatch(room, opponent, opponent == null ? ActorType.BOT : ActorType.HUMAN, "FORFEIT");
    }

    public void finishMatch(Room room, Long winnerId, ActorType winnerType, String reason) {
        historyController.finishMatch(room.getMatchId(), winnerId, winnerType.name(), reason);
        RoomManager.getInstance().removeRoom(room.getMatchId());
        
        if (room.getPlayer1Id() > 0) ClientManager.getInstance().setStatus(room.getPlayer1Id(), PlayerStatus.IDLE);
        if (room.getPlayer2Id() != null) ClientManager.getInstance().setStatus(room.getPlayer2Id(), PlayerStatus.IDLE);
        
        broadcast(room, Message.of(MessageType.MATCH_FINISHED, winnerId != null ? winnerId.toString() : "BOT"));
    }
    
    private void broadcast(Room room, Message message) {
        ClientManager.getInstance().sendToUser(room.getPlayer1Id(), message);
        if (room.getPlayer2Id() != null) {
            ClientManager.getInstance().sendToUser(room.getPlayer2Id(), message);
        }
    }
}

