package controllers;

import database.DBConnection;
import protocol.Message;
import protocol.MessageType;
import services.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RankingController {

    public void handleMessage(Client client, Message message) {
        if (message.getType() == MessageType.GET_RANKING) {
            List<String> rankings = getRankingPvp();
            client.send(Message.of(MessageType.RANKING_RESULT, rankings));
        } else if (message.getType() == MessageType.GET_HISTORY) {
            // Assume payload is the user's ID
            long userId = client.getLoggedInUserId();
            List<String> history = getHistory(userId);
            client.send(Message.of(MessageType.HISTORY_RESULT, history));
        }
    }

    private List<String> getRankingPvp() {
        List<String> rankings = new ArrayList<>();
        // Query to get user and their PvP win counts
        String sql = "SELECT u.username, COUNT(m.id) as wins " +
                     "FROM users u " +
                     "LEFT JOIN matches m ON u.id = m.winner_id AND m.mode = 'PVP' " +
                     "GROUP BY u.id " +
                     "ORDER BY wins DESC, u.username ASC " +
                     "LIMIT 100";
                     
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
             
            int rank = 1;
            while (rs.next()) {
                String username = rs.getString("username");
                int wins = rs.getInt("wins");
                // Format: rank|username|wins
                rankings.add(rank + "|" + username + "|" + wins);
                rank++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rankings;
    }

    private List<String> getHistory(long userId) {
        List<String> history = new ArrayList<>();
        String sql = "SELECT m.id, m.mode, " +
                     "CASE WHEN m.player1_id = ? THEN u2.username ELSE u1.username END as opponent_name, " +
                     "CASE WHEN m.winner_id = ? THEN 'WIN' WHEN m.winner_id IS NULL THEN 'DRAW_OR_BOT' ELSE 'LOSS' END as result, " +
                     "m.end_reason, m.ended_at " +
                     "FROM matches m " +
                     "LEFT JOIN users u1 ON m.player1_id = u1.id " +
                     "LEFT JOIN users u2 ON m.player2_id = u2.id " +
                     "WHERE (m.player1_id = ? OR m.player2_id = ?) AND m.ended_at IS NOT NULL " +
                     "ORDER BY m.ended_at DESC LIMIT 50";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            stmt.setLong(3, userId);
            stmt.setLong(4, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String matchId = rs.getString("id");
                    String mode = rs.getString("mode");
                    String opponentName = rs.getString("opponent_name");
                    if (opponentName == null) opponentName = "BOT";
                    String result = rs.getString("result");
                    String endReason = rs.getString("end_reason");
                    String endedAt = rs.getString("ended_at");
                    
                    // Format: matchId|mode|opponentName|result|endReason|endedAt
                    history.add(matchId + "|" + mode + "|" + opponentName + "|" + result + "|" + endReason + "|" + endedAt);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return history;
    }
}
