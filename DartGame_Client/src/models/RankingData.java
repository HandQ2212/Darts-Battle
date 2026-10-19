package models;

public class RankingData {
    private int rank;
    private String username;
    private int pvpWins;

    public RankingData(int rank, String username, int pvpWins) {
        this.rank = rank;
        this.username = username;
        this.pvpWins = pvpWins;
    }

    public int getRank() { return rank; }
    public String getUsername() { return username; }
    public int getPvpWins() { return pvpWins; }
}
