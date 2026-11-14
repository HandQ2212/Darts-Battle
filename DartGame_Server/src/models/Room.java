package models;

import enums.ActorType;
import enums.MatchMode;
import enums.BotDifficulty;

public class Room {
    public String player1Name;
    public String player2Name;
    public long version;
    public Long winnerId;
    public ActorType winnerType;
    public String endReason;
    public long deadline;
    public volatile boolean started;
    // All accesses occur under this room's monitor; retain only this turn's commands.
    public final java.util.Map<String, Object> requests = new java.util.HashMap<>();
    public final java.util.LinkedHashMap<String,Boolean> sessionRequests=new java.util.LinkedHashMap<>();
    private volatile String matchId;
    private final String sessionId;
    private int roundIndex=1;
    private volatile String sessionPhase="PLAYING";
    private boolean rematchSupported,player1Ready,player2Ready;
    public boolean startingNext;
    private long player1Id;
    private Long player2Id;
    private ActorType player2Type;
    private MatchMode mode;
    private GameState gameState;
    private BotDifficulty botDifficulty=BotDifficulty.EASY;

    public Room(String matchId, long player1Id, Long player2Id, ActorType player2Type, MatchMode mode) {
        this.matchId = matchId;
        this.sessionId = matchId;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.player2Type = player2Type;
        this.mode = mode;
        this.gameState = new GameState();
    }

    public String getMatchId() { return matchId; }
    public String getSessionId(){return sessionId;}
    public int getRoundIndex(){return roundIndex;}
    public String getSessionPhase(){return sessionPhase;}
    public void setSessionPhase(String value){sessionPhase=value;}
    public boolean isRematchSupported(){return rematchSupported;}
    public void setRematchSupported(boolean value){rematchSupported=value;}
    public boolean isPlayer1Ready(){return player1Ready;}
    public boolean isPlayer2Ready(){return player2Ready;}
    public void setReady(long userId,boolean value){
        if(userId==player1Id)player1Ready=value;
        else if(player2Id!=null&&userId==player2Id)player2Ready=value;
        else throw new IllegalArgumentException("Bạn không thuộc phiên chơi.");
    }
    public void beginNextMatch(String id){
        matchId=id;roundIndex++;gameState=new GameState();started=false;version=0;winnerId=null;winnerType=null;endReason=null;deadline=0;
        player1Ready=false;player2Ready=false;startingNext=false;sessionPhase="PLAYING";requests.clear();
    }
    public void resetReady(){player1Ready=false;player2Ready=false;startingNext=false;}
    public long getPlayer1Id() { return player1Id; }
    public Long getPlayer2Id() { return player2Id; }
    public ActorType getPlayer2Type() { return player2Type; }
    public MatchMode getMode() { return mode; }
    public GameState getGameState() { return gameState; }
    public BotDifficulty getBotDifficulty(){return botDifficulty;}
    public void setBotDifficulty(BotDifficulty value){botDifficulty=java.util.Objects.requireNonNull(value);}

    public boolean containsUser(long userId) {
        return player1Id == userId || (player2Id != null && player2Id == userId);
    }

    public Long getOpponentUserId(long userId) {
        if (player1Id == userId) return player2Id;
        if (player2Id != null && player2Id == userId) return player1Id;
        return null;
    }
}
