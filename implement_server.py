import os

files = {
    "DartGame_Server/src/models/GameState.java": """package models;

import enums.ActorType;
import enums.MatchStatus;

public class GameState {
    private int player1RemainingScore = 301;
    private int player2RemainingScore = 301;
    private Long currentPlayerId;
    private ActorType currentActorType;
    private long turnId = 1;
    private int dartsThrown = 0;
    private int rotationDegree = 0;
    private MatchStatus status = MatchStatus.WAITING_ROTATION;
    private int currentTurnScore = 0;

    public GameState() {}

    public synchronized int getPlayer1RemainingScore() { return player1RemainingScore; }
    public synchronized void setPlayer1RemainingScore(int score) { this.player1RemainingScore = score; }

    public synchronized int getPlayer2RemainingScore() { return player2RemainingScore; }
    public synchronized void setPlayer2RemainingScore(int score) { this.player2RemainingScore = score; }

    public synchronized Long getCurrentPlayerId() { return currentPlayerId; }
    public synchronized void setCurrentPlayerId(Long currentPlayerId) { this.currentPlayerId = currentPlayerId; }

    public synchronized ActorType getCurrentActorType() { return currentActorType; }
    public synchronized void setCurrentActorType(ActorType currentActorType) { this.currentActorType = currentActorType; }

    public synchronized long getTurnId() { return turnId; }
    public synchronized void setTurnId(long turnId) { this.turnId = turnId; }

    public synchronized int getDartsThrown() { return dartsThrown; }
    public synchronized void setDartsThrown(int dartsThrown) { this.dartsThrown = dartsThrown; }

    public synchronized int getRotationDegree() { return rotationDegree; }
    public synchronized void setRotationDegree(int rotationDegree) { this.rotationDegree = rotationDegree; }

    public synchronized MatchStatus getStatus() { return status; }
    public synchronized void setStatus(MatchStatus status) { this.status = status; }

    public synchronized int getCurrentTurnScore() { return currentTurnScore; }
    public synchronized void setCurrentTurnScore(int score) { this.currentTurnScore = score; }
}
""",
    "DartGame_Server/src/models/Room.java": """package models;

import enums.ActorType;
import enums.MatchMode;

public class Room {
    private String matchId;
    private long player1Id;
    private Long player2Id;
    private ActorType player2Type;
    private MatchMode mode;
    private GameState gameState;

    public Room(String matchId, long player1Id, Long player2Id, ActorType player2Type, MatchMode mode) {
        this.matchId = matchId;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.player2Type = player2Type;
        this.mode = mode;
        this.gameState = new GameState();
    }

    public String getMatchId() { return matchId; }
    public long getPlayer1Id() { return player1Id; }
    public Long getPlayer2Id() { return player2Id; }
    public ActorType getPlayer2Type() { return player2Type; }
    public MatchMode getMode() { return mode; }
    public GameState getGameState() { return gameState; }

    public boolean containsUser(long userId) {
        return player1Id == userId || (player2Id != null && player2Id == userId);
    }

    public Long getOpponentUserId(long userId) {
        if (player1Id == userId) return player2Id;
        if (player2Id != null && player2Id == userId) return player1Id;
        return null;
    }
}
""",
    "DartGame_Server/src/services/ClientManager.java": """package services;

import enums.PlayerStatus;
import protocol.Message;
import protocol.MessageType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class ClientManager {
    private static final ClientManager instance = new ClientManager();
    private final ConcurrentHashMap<Long, Client> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, PlayerStatus> statuses = new ConcurrentHashMap<>();

    private ClientManager() {}
    public static ClientManager getInstance() { return instance; }

    public void addClient(Client client) {
        if (client.getLoggedInUserId() != -1) {
            clients.put(client.getLoggedInUserId(), client);
            statuses.put(client.getLoggedInUserId(), PlayerStatus.IDLE);
            broadcastOnlineList();
        }
    }

    public void removeClient(long userId) {
        clients.remove(userId);
        statuses.remove(userId);
        broadcastOnlineList();
    }

    public Client getClient(long userId) {
        return clients.get(userId);
    }

    public void setStatus(long userId, PlayerStatus status) {
        if (statuses.containsKey(userId)) {
            statuses.put(userId, status);
            broadcastOnlineList();
        }
    }

    public PlayerStatus getStatus(long userId) {
        return statuses.get(userId);
    }

    public void sendToUser(long userId, Message message) {
        Client client = clients.get(userId);
        if (client != null) {
            client.send(message);
        }
    }

    public void broadcastOnlineList() {
        // Implementation to build online list and broadcast
    }
}
""",
    "DartGame_Server/src/services/RoomManager.java": """package services;

import models.Room;
import enums.ActorType;
import enums.MatchMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class RoomManager {
    private static final RoomManager instance = new RoomManager();
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userRoomMap = new ConcurrentHashMap<>();

    private RoomManager() {}
    public static RoomManager getInstance() { return instance; }

    public Room createPvpRoom(long p1, long p2) {
        String matchId = UUID.randomUUID().toString();
        Room room = new Room(matchId, p1, p2, ActorType.HUMAN, MatchMode.PVP);
        rooms.put(matchId, room);
        userRoomMap.put(p1, matchId);
        userRoomMap.put(p2, matchId);
        return room;
    }

    public Room createPveRoom(long p1) {
        String matchId = UUID.randomUUID().toString();
        Room room = new Room(matchId, p1, null, ActorType.BOT, MatchMode.PVE);
        rooms.put(matchId, room);
        userRoomMap.put(p1, matchId);
        return room;
    }

    public Room getRoom(String matchId) {
        return rooms.get(matchId);
    }

    public Room findRoomOfUser(long userId) {
        String matchId = userRoomMap.get(userId);
        if (matchId != null) {
            return rooms.get(matchId);
        }
        return null;
    }

    public void removeRoom(String matchId) {
        Room room = rooms.remove(matchId);
        if (room != null) {
            userRoomMap.remove(room.getPlayer1Id());
            if (room.getPlayer2Id() != null) {
                userRoomMap.remove(room.getPlayer2Id());
            }
        }
    }
}
""",
    "DartGame_Server/src/services/GameService.java": """package services;

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
""",
    "DartGame_Server/src/services/BotService.java": """package services;

import protocol.dto.ThrowCoordinateRequest;
import java.util.Random;

public class BotService {
    private static final BotService instance = new BotService();
    private final Random random = new Random();
    
    private BotService() {}
    public static BotService getInstance() { return instance; }

    public void playTurn(String matchId) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                int rotation = random.nextInt(359) + 1;
                // apply rotation
                
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(1500);
                    // generate coordinate
                    double x = random.nextInt(200) - 100;
                    double y = random.nextInt(200) - 100;
                    ThrowCoordinateRequest req = new ThrowCoordinateRequest(matchId, 0, x, y);
                    GameService.getInstance().submitThrow(req, -1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
"""
}

for filepath, content in files.items():
    with open(filepath, 'w') as f:
        f.write(content)

print("Server logic implemented.")
