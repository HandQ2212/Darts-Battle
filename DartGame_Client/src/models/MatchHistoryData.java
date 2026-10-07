package models;

public class MatchHistoryData {
    private String matchId;
    private String mode;
    private String opponentName;
    private String result;
    private String endReason;
    private String endedAt;

    public MatchHistoryData(String matchId, String mode, String opponentName, String result, String endReason, String endedAt) {
        this.matchId = matchId;
        this.mode = mode;
        this.opponentName = opponentName;
        this.result = result;
        this.endReason = endReason;
        this.endedAt = endedAt;
    }

    public String getMatchId() { return matchId; }
    public String getMode() { return mode; }
    public String getOpponentName() { return opponentName; }
    public String getResult() { return result; }
    public String getEndReason() { return endReason; }
    public String getEndedAt() { return endedAt; }
}
