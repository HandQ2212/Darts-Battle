package models;

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
