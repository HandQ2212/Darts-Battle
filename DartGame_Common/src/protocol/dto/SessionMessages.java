package protocol.dto;
import enums.BotDifficulty;
import java.io.Serializable;
public final class SessionMessages {
    private SessionMessages(){}
    public record State(String sessionId,String matchId,String phase,int round,boolean rematchSupported,
                        boolean player1Ready,boolean player2Ready,BotDifficulty botDifficulty,String notice) implements Serializable {}
    public record ReadyRequest(String sessionId,String matchId,String requestId,boolean ready) implements Serializable {}
    public record LeaveRequest(String sessionId,String requestId) implements Serializable {}
}
