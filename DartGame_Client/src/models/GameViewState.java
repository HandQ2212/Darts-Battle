package models;

import enums.ActorType;
import enums.MatchMode;
import protocol.dto.ThrowResolvedResponse;

public class GameViewState {
    private String matchId;
    private MatchMode mode;
    private String playerName;
    private String opponentName;
    private int playerRemaining = 301;
    private int opponentRemaining = 301;
    private String currentActorName;
    private ActorType currentActorType;
    private long turnId = 1;
    private int rotationDegree = 0;
    private int dartsThrown = 0;

    public void startMatch(String matchId, MatchMode mode, String playerName, String opponentName) {
        this.matchId = matchId;
        this.mode = mode;
        this.playerName = playerName;
        this.opponentName = opponentName;
        this.playerRemaining = 301;
        this.opponentRemaining = 301;
        this.turnId = 1;
        this.dartsThrown = 0;
        this.rotationDegree = 0;
        this.currentActorName = playerName;
        this.currentActorType = ActorType.HUMAN;
    }

    public void startTurn(long turnId) {
        this.turnId = turnId;
        this.dartsThrown = 0;
    }

    public void applyThrow(ThrowResolvedResponse result) {
        this.dartsThrown = result.getDartIndex();
        this.rotationDegree = result.getRotationDegree();
        if (result.getActorName().equals(playerName)) {
            this.playerRemaining = result.getRemainingScoreAfter();
        } else {
            this.opponentRemaining = result.getRemainingScoreAfter();
        }
    }

    // Getters and Setters
    public String getMatchId() { return matchId; }
    public MatchMode getMode() { return mode; }
    public String getPlayerName() { return playerName; }
    public String getOpponentName() { return opponentName; }
    public int getPlayerRemaining() { return playerRemaining; }
    public int getOpponentRemaining() { return opponentRemaining; }
    public String getCurrentActorName() { return currentActorName; }
    public void setCurrentActorName(String currentActorName) { this.currentActorName = currentActorName; }
    public ActorType getCurrentActorType() { return currentActorType; }
    public void setCurrentActorType(ActorType currentActorType) { this.currentActorType = currentActorType; }
    public long getTurnId() { return turnId; }
    public int getRotationDegree() { return rotationDegree; }
    public void setRotationDegree(int rotationDegree) { this.rotationDegree = rotationDegree; }
    public int getDartsThrown() { return dartsThrown; }
}
