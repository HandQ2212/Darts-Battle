package models;

import enums.PlayerStatus;

public class User {
    private long id;
    private String username;
    private PlayerStatus status;

    public User(long id, String username, String statusStr) {
        this.id = id;
        this.username = username;
        if ("online".equalsIgnoreCase(statusStr)) {
            this.status = PlayerStatus.IDLE;
        } else {
            this.status = PlayerStatus.OFFLINE;
        }
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public PlayerStatus getStatus() { return status; }
    public void setStatus(PlayerStatus status) { this.status = status; }
}
