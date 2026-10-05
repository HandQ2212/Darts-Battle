package models;

public class MatchHistoryData {
    public String username;
    public String opponent;
    public String result;
    public String time;

    public MatchHistoryData(String username, String opponent, String result, String time) {
        this.username = username;
        this.opponent = opponent;
        this.result = result;
        this.time = time;
    }
}
