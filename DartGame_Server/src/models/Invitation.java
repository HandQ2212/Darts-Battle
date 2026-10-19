package models;

import java.time.LocalDateTime;

public class Invitation {
    public enum Status {
        PENDING, ACCEPTED, REJECTED, EXPIRED
    }

    private String invitationId;
    private long senderUserId;
    private long receiverUserId;
    private LocalDateTime expiresAt;
    private Status status;

    public Invitation(String invitationId, long senderUserId, long receiverUserId, LocalDateTime expiresAt) {
        this.invitationId = invitationId;
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.expiresAt = expiresAt;
        this.status = Status.PENDING;
    }

    public String getInvitationId() { return invitationId; }
    public long getSenderUserId() { return senderUserId; }
    public long getReceiverUserId() { return receiverUserId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
