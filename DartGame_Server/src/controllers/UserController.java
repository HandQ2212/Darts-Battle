package controllers;
import protocol.*;
import protocol.dto.LoginRequest;
import protocol.dto.GameMessages.AuthResponse;
import services.*;
import database.DBConnection;
import java.sql.*;

public class UserController {
    public void handleMessage(Client client, Message message) {
        if (message.getType() == MessageType.LOGOUT_REQUEST) {
            network.DiagnosticLog.event("logout_requested","userId="+client.getLoggedInUserId());
            LobbyService.getInstance().logout(client, "FORFEIT");
            client.send(Message.of(MessageType.LOGOUT_RESULT, "SUCCESS"));
            return;
        }
        if (client.getLoggedInUserId() != -1) throw new IllegalArgumentException("Bạn đã đăng nhập.");
        LoginRequest req = (LoginRequest)message.getPayload();
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String password = req.getPassword();
        if (!username.matches("[A-Za-z0-9_]{3,32}") || password == null || password.length() < 6 || password.length() > 128)
            throw new IllegalArgumentException("Tên 3–32 ký tự chữ/số/_, mật khẩu 6–128 ký tự.");
        MessageType response = message.getType() == MessageType.REGISTER_REQUEST ? MessageType.REGISTER_RESULT : MessageType.LOGIN_RESULT;
        try (Connection c = DBConnection.getInstance().getConnection()) {
            if (message.getType() == MessageType.REGISTER_REQUEST) {
                try (PreparedStatement s = c.prepareStatement("INSERT INTO users(username,password_hash) VALUES (?,?)")) {
                    s.setString(1, username); s.setString(2, PasswordHasher.hash(password)); s.executeUpdate();
                }
                client.send(Message.of(response, new AuthResponse(true, -1, username, "Đăng ký thành công. Hãy đăng nhập.")));
            } else {
                try (PreparedStatement s = c.prepareStatement("SELECT id,username,password_hash FROM users WHERE username=?")) {
                    s.setString(1, username);
                    try (ResultSet r = s.executeQuery()) {
                        if (!r.next() || !PasswordHasher.verify(password, r.getString("password_hash"))) {
                            client.send(Message.of(response, new AuthResponse(false, -1, username, "Sai tên đăng nhập hoặc mật khẩu."))); return;
                        }
                        LobbyService.getInstance().login(client, r.getLong("id"), r.getString("username"));
                    }
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            client.send(Message.of(response, new AuthResponse(false, -1, username, "Tên đăng nhập đã tồn tại.")));
        } catch (SQLException e) {
            System.err.println("Authentication DB failure: " + e.getMessage());
            client.send(Message.of(response, new AuthResponse(false, -1, username, "Không truy cập được database. Hãy thử lại.")));
        }
    }
}
