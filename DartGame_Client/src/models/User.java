package models;

public class User {
    private int id;
    private String username;
    private String status;

    public User(int id, String username, String status) {
        this.id = id;
        this.username = username;
        this.status = status;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return username + " (" + status + ")";
    }
}
