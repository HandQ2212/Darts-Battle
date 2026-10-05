package protocol.dto;

import enums.ActorType;
import enums.HitArea;

public class ThrowResolvedResponse {
    private String matchId;
    private long turnId;
    private String actorName;
    private ActorType actorType;
    private int dartIndex;
    private double x;
    private double y;
    private int rotationDegree;
    private HitArea hitArea;
    private int dartScore;
    private int remainingScoreAfter;
    private boolean isBust;
    private boolean isTurnFinished;
    private boolean isMatchFinished;

    public ThrowResolvedResponse() {}

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public long getTurnId() { return turnId; }
    public void setTurnId(long turnId) { this.turnId = turnId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public ActorType getActorType() { return actorType; }
    public void setActorType(ActorType actorType) { this.actorType = actorType; }

    public int getDartIndex() { return dartIndex; }
    public void setDartIndex(int dartIndex) { this.dartIndex = dartIndex; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public int getRotationDegree() { return rotationDegree; }
    public void setRotationDegree(int rotationDegree) { this.rotationDegree = rotationDegree; }

    public HitArea getHitArea() { return hitArea; }
    public void setHitArea(HitArea hitArea) { this.hitArea = hitArea; }

    public int getDartScore() { return dartScore; }
    public void setDartScore(int dartScore) { this.dartScore = dartScore; }

    public int getRemainingScoreAfter() { return remainingScoreAfter; }
    public void setRemainingScoreAfter(int remainingScoreAfter) { this.remainingScoreAfter = remainingScoreAfter; }

    public boolean isBust() { return isBust; }
    public void setBust(boolean bust) { isBust = bust; }

    public boolean isTurnFinished() { return isTurnFinished; }
    public void setTurnFinished(boolean turnFinished) { isTurnFinished = turnFinished; }

    public boolean isMatchFinished() { return isMatchFinished; }
    public void setMatchFinished(boolean matchFinished) { isMatchFinished = matchFinished; }
}
