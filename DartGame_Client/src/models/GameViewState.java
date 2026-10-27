package models;
import enums.*;
import protocol.dto.GameMessages.MatchSnapshot;
/** UI projection of the server snapshot. No local score/turn decisions. */
public class GameViewState {
    private MatchSnapshot snapshot;
    private long localUserId;
    public void begin(MatchSnapshot value,long userId) { snapshot=value; localUserId=userId; }
    public boolean apply(MatchSnapshot value) {
        if(snapshot==null || !snapshot.matchId().equals(value.matchId()) || value.version()<=snapshot.version()) return false;
        snapshot=value; return true;
    }
    public void reset() { snapshot=null; }
    public MatchSnapshot getSnapshot() { return snapshot; }
    public String getMatchId() { return snapshot==null?null:snapshot.matchId(); }
    public long getTurnId() { return snapshot==null?0:snapshot.turnId(); }
    public int getDartsThrown() { return snapshot==null?0:snapshot.dartsThrown(); }
    public boolean isMyTurn() { return snapshot!=null && snapshot.actorType()==ActorType.HUMAN && java.util.Objects.equals(snapshot.actorId(),localUserId); }
    public boolean isPlayer1() { return snapshot!=null && snapshot.player1Id()==localUserId; }
    public String getOpponentName() { return snapshot==null?"":isPlayer1()?snapshot.player2Name():snapshot.player1Name(); }
}
