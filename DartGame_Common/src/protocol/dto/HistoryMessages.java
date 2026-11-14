package protocol.dto;

import java.io.Serializable;
import java.util.ArrayList;

/** Typed responses; only sent to clients that register FEATURE_SUPPORT. */
public final class HistoryMessages {
    private HistoryMessages() {}
    public record Rank(long userId,String name,int wins,int rank) implements Serializable {}
    public record Rankings(ArrayList<Rank> rows,String error) implements Serializable {}
    public record PageRequest(long playerId,int page,boolean publicOnly) implements Serializable {}
    public record Match(String matchId,String mode,long player1Id,String player1Name,Long player2Id,String player2Name,
                        Long winnerId,String winnerType,String reason,String endedAt,Integer score1,Integer score2,
                        String difficulty,boolean reconstructed,boolean incomplete) implements Serializable {}
    public record Page(long playerId,int page,boolean hasNext,ArrayList<Match> matches,String error) implements Serializable {}
    public record DetailRequest(String matchId) implements Serializable {}
    public record Dart(long turn,int index,Long playerId,String actor,int score,int remaining,Boolean bust) implements Serializable {}
    public record Detail(Match match,ArrayList<Dart> darts,String error) implements Serializable {}
}
