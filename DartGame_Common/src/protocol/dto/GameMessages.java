package protocol.dto;
import java.io.Serializable;
import enums.*;

/** Version 2 wire payloads. Only the server creates authoritative snapshots. */
public final class GameMessages {
    private GameMessages() {}
    public record AuthResponse(boolean success, long userId, String username, String message) implements Serializable {}
    public record OnlineUser(long id, String username, PlayerStatus status) implements Serializable {}
    public record InviteResponse(String id, long senderId, String senderName, long expiresAt) implements Serializable {}
    public record RotationRequest(String matchId, long turnId, int degrees, String requestId) implements Serializable {}
    public record ChatMessage(String matchId, String text, String senderName) implements Serializable {}
    public record MatchSnapshot(String matchId, MatchMode mode, long player1Id, String player1Name,
            Long player2Id, String player2Name, Long actorId, ActorType actorType, long turnId,
            int dartsThrown, int rotationDegree, int player1Score, int player2Score,
            MatchStatus status, long version, Long winnerId, ActorType winnerType,
            String endReason, long deadline) implements Serializable {}
}
