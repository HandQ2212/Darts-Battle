package models;

import enums.ActorType;
import enums.MatchMode;

public class Room {
    public String player1Name;
    public String player2Name;
    public long version;
    public Long winnerId;
    public ActorType winnerType;
    public String endReason;
    public long deadline;
    public boolean started;
    // All accesses occur under this room's monitor; retain only this turn's commands.
    public final java.util.Map<String, Object> requests = new java.util.HashMap<>();
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
