package protocol.dto;

import java.io.Serializable;

public class ThrowCoordinateRequest implements Serializable {
    private String matchId;
    private long turnId;
    private double x;
    private double y;

    public ThrowCoordinateRequest() {}

    public ThrowCoordinateRequest(String matchId, long turnId, double x, double y) {
        this.matchId = matchId;
        this.turnId = turnId;
        this.x = x;
        this.y = y;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public long getTurnId() {
        return turnId;
    }

    public void setTurnId(long turnId) {
        this.turnId = turnId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
