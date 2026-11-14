package protocol.dto;

import java.io.Serializable;
import java.util.ArrayList;

/** Signaling-only payloads. Media bytes never pass through the game server. */
public final class MediaMessages {
    private MediaMessages() {}

    public record JoinRequest(String matchId) implements Serializable {}
    public record Signal(String matchId, String callId, String descriptionType, String value,
                         String mid, int mLineIndex, long senderId) implements Serializable {}
    public record PeerState(String matchId, String callId, String peerName,
                            boolean peerJoined, boolean offerer, String notice) implements Serializable {}
    public record IceConfig(String matchId, String callId, ArrayList<IceServer> servers,
                            boolean turnAvailable) implements Serializable {}
    public record IceServer(ArrayList<String> urls, String username, String credential) implements Serializable {}
}
