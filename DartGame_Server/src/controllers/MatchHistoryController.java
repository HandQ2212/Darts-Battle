package controllers;

import database.DBConnection;
import models.ThrowResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MatchHistoryController {
    
    // Create a new match record
    public void createMatch(String matchId, String mode, long player1Id, Long player2Id, String player2Type) {
        String sql = "INSERT INTO matches (id, mode, player1_id, player2_id, player2_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, matchId);
            stmt.setString(2, mode);
            stmt.setLong(3, player1Id);
            if (player2Id != null) {
                stmt.setLong(4, player2Id);
            } else {
                stmt.setNull(4, java.sql.Types.BIGINT);
            }
            stmt.setString(5, player2Type);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Save a throw result
    public void saveThrow(ThrowResult result, Long playerId) {
        String sql = "INSERT INTO match_throws (match_id, player_id, actor_type, turn_number, dart_number, x, y, rotation_degree, hit_area, dart_score, remaining_score_after) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, result.getMatchId());
            if (playerId != null) {
                stmt.setLong(2, playerId);
            } else {
                stmt.setNull(2, java.sql.Types.BIGINT);
            }
            stmt.setString(3, result.getActorType().name());
            stmt.setLong(4, result.getTurnId());
            stmt.setInt(5, result.getDartIndex());
            stmt.setDouble(6, result.getX());
            stmt.setDouble(7, result.getY());
            stmt.setInt(8, result.getRotationDegree());
            stmt.setString(9, result.getHitArea().name());
            stmt.setInt(10, result.getDartScore());
            stmt.setInt(11, result.getRemainingScoreAfter());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Finish match
    public void finishMatch(String matchId, Long winnerId, String winnerType, String endReason) {
        String sql = "UPDATE matches SET winner_id = ?, winner_type = ?, end_reason = ?, ended_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (winnerId != null) {
                stmt.setLong(1, winnerId);
            } else {
                stmt.setNull(1, java.sql.Types.BIGINT);
            }
            stmt.setString(2, winnerType);
            stmt.setString(3, endReason);
            stmt.setString(4, matchId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
