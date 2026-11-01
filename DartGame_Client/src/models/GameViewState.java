package models;
import enums.*;
import protocol.dto.GameMessages.MatchSnapshot;
import protocol.dto.ThrowResolvedResponse;
import java.util.*;
/** UI projection of the server snapshot. No local score/turn decisions. */
public class GameViewState {
    private MatchSnapshot snapshot;
    private long localUserId;
    public record Dart(int score, boolean bust, int remaining) {}
    public static final class Turn {
        private final long turnId;
        private final boolean own;
        private final Dart[] darts=new Dart[3];
        private Turn(long turnId,boolean own){this.turnId=turnId;this.own=own;}
        public long turnId(){return turnId;}
        public boolean own(){return own;}
        public List<Dart> darts(){return Collections.unmodifiableList(Arrays.asList(darts.clone()));}
        public int deducted(){int total=0;for(Dart d:darts)if(d!=null&&!d.bust())total+=d.score();return total;}
        public int remaining(){for(int i=2;i>=0;i--)if(darts[i]!=null)return darts[i].remaining();return 301;}
    }
    private final LinkedHashMap<Long,Turn> history=new LinkedHashMap<>();
    public void begin(MatchSnapshot value,long userId) { history.clear();snapshot=value; localUserId=userId; }
    public boolean apply(MatchSnapshot value) {
        if(snapshot==null || !snapshot.matchId().equals(value.matchId()) || value.version()<=snapshot.version()) return false;
        snapshot=value; return true;
    }
    public boolean applyThrow(ThrowResolvedResponse result) {
        if(result==null||snapshot==null||result.getSnapshot()==null||!Objects.equals(snapshot.matchId(),result.getMatchId())
                ||result.getDartIndex()<1||result.getDartIndex()>3)return false;
        Turn existing=history.get(result.getTurnId());
        if(existing!=null&&existing.darts[result.getDartIndex()-1]!=null)return false;
        if(!apply(result.getSnapshot()))return false;
        Turn turn=history.computeIfAbsent(result.getTurnId(),id->new Turn(id,Objects.equals(result.getActorId(),localUserId)));
        turn.darts[result.getDartIndex()-1]=new Dart(result.getDartScore(),result.isBust(),result.getRemainingScoreAfter());
        return true;
    }
    public List<Turn> history(boolean own){return history.values().stream().filter(turn->turn.own()==own).toList();}
    public void reset() { snapshot=null;history.clear(); }
    public MatchSnapshot getSnapshot() { return snapshot; }
    public String getMatchId() { return snapshot==null?null:snapshot.matchId(); }
    public long getTurnId() { return snapshot==null?0:snapshot.turnId(); }
    public int getDartsThrown() { return snapshot==null?0:snapshot.dartsThrown(); }
    public boolean isMyTurn() { return snapshot!=null && snapshot.actorType()==ActorType.HUMAN && java.util.Objects.equals(snapshot.actorId(),localUserId); }
    public boolean isPlayer1() { return snapshot!=null && snapshot.player1Id()==localUserId; }
    public String getOpponentName() { return snapshot==null?"":isPlayer1()?snapshot.player2Name():snapshot.player1Name(); }
}
