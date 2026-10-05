package controller;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller để quản lý bảng xếp hạng (leaderboard)
 * Chỉ sử dụng bảng users với cột score có sẵn - KHÔNG tạo thêm bảng mới
 * 
 * @author DartGame Team
 */
public class LeaderboardController {
    
    // SQL Queries - CHỈ SỬ DỤNG BẢNG USERS
    private static final String GET_TOP_PLAYERS = 
        "SELECT id, username, score " +
        "FROM users " +
        "ORDER BY score DESC " +
        "LIMIT ?";
    
    private static final String GET_USER_RANK = 
        "SELECT COUNT(*) + 1 as `rank` " +
        "FROM users " +
        "WHERE score > (SELECT score FROM users WHERE username = ? LIMIT 1)";
    
    private static final String GET_USER_STATS = 
        "SELECT id, username, score " +
        "FROM users " +
        "WHERE username = ?";
    
    // Database connection
    private final Connection con;
    
    /**
     * Constructor khởi tạo kết nối database
     */
    public LeaderboardController() {
        this.con = DBConnection.getInstance().getConnection();
    }
    
    /**
     * Lấy danh sách top người chơi theo điểm số
     * 
     * @param limit Số lượng người chơi muốn lấy (mặc định 100)
     * @return Chuỗi JSON chứa thông tin các người chơi
     */
    public String getTopPlayers(int limit) {
        StringBuilder result = new StringBuilder();
        result.append("success;");
        
        try {
            PreparedStatement p = con.prepareStatement(GET_TOP_PLAYERS);
            p.setInt(1, limit);
            ResultSet r = p.executeQuery();
            
            List<String> players = new ArrayList<>();
            while (r.next()) {
                // Format đơn giản: userId|username|score
                String playerData = String.format("%d|%s|%d",
                    r.getInt("id"),
                    r.getString("username"),
                    r.getInt("score")
                );
                players.add(playerData);
            }
            
            result.append(players.size()).append(";");
            for (String player : players) {
                result.append(player).append(";");
            }
            
            r.close();
            p.close();
            
            System.out.println("✅ Retrieved " + players.size() + " players from leaderboard");
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting top players: " + e.getMessage());
            e.printStackTrace();
            return "failed;Database error: " + e.getMessage();
        }
        
        return result.toString();
    }
    
    /**
     * Lấy thứ hạng của một người chơi cụ thể
     * 
     * @param username Tên người chơi
     * @return Chuỗi chứa thứ hạng hoặc thông báo lỗi
     */
    public String getUserRank(String username) {
        try {
            // Kiểm tra xem user có tồn tại không
            PreparedStatement checkUser = con.prepareStatement(
                "SELECT COUNT(*) as count FROM users WHERE username = ?"
            );
            checkUser.setString(1, username);
            ResultSet checkResult = checkUser.executeQuery();
            
            if (checkResult.next() && checkResult.getInt("count") == 0) {
                checkResult.close();
                checkUser.close();
                return "failed;User not found";
            }
            checkResult.close();
            checkUser.close();
            
            // Lấy thứ hạng
            PreparedStatement p = con.prepareStatement(GET_USER_RANK);
            p.setString(1, username);
            
            ResultSet r = p.executeQuery();
            
            if (r.next()) {
                int rank = r.getInt("rank");
                r.close();
                p.close();
                System.out.println("✅ User '" + username + "' rank: " + rank);
                return "success;" + rank;
            }
            
            r.close();
            p.close();
            return "failed;Could not calculate rank";
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting user rank: " + e.getMessage());
            e.printStackTrace();
            return "failed;Database error: " + e.getMessage();
        }
    }
    
    /**
     * Lấy thống kê chi tiết của một người chơi
     * 
     * @param username Tên người chơi
     * @return Chuỗi chứa thông tin thống kê
     */
    public String getUserStats(String username) {
        try {
            PreparedStatement p = con.prepareStatement(GET_USER_STATS);
            p.setString(1, username);
            ResultSet r = p.executeQuery();
            
            if (r.next()) {
                // Format đơn giản: username|score
                String stats = String.format("success;%s|%d",
                    r.getString("username"),
                    r.getInt("score")
                );
                
                r.close();
                p.close();
                return stats;
            }
            
            r.close();
            p.close();
            return "failed;User not found";
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting user stats: " + e.getMessage());
            e.printStackTrace();
            return "failed;Database error: " + e.getMessage();
        }
    }
    
    /**
     * Xử lý request từ client để lấy bảng xếp hạng
     * Format request: "GET_LEADERBOARD;limit" hoặc "GET_LEADERBOARD" (mặc định 100)
     * 
     * @param request Chuỗi request từ client
     * @return Chuỗi response gửi về client
     */
    public String handleLeaderboardRequest(String request) {
        try {
            String[] parts = request.split(";");
            int limit = 100; // Mặc định lấy top 100
            
            if (parts.length > 1) {
                try {
                    limit = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    limit = 100;
                }
            }
            
            return getTopPlayers(limit);
            
        } catch (Exception e) {
            System.err.println("❌ Error handling leaderboard request: " + e.getMessage());
            e.printStackTrace();
            return "failed;Error processing request";
        }
    }
    
    /**
     * Xử lý request từ client để lấy thứ hạng của user
     * Format request: "GET_USER_RANK;username"
     * 
     * @param request Chuỗi request từ client
     * @return Chuỗi response gửi về client
     */
    public String handleUserRankRequest(String request) {
        try {
            String[] parts = request.split(";");
            
            if (parts.length < 2) {
                return "failed;Missing username parameter";
            }
            
            String username = parts[1];
            return getUserRank(username);
            
        } catch (Exception e) {
            System.err.println("❌ Error handling user rank request: " + e.getMessage());
            e.printStackTrace();
            return "failed;Error processing request";
        }
    }
    
    /**
     * Xử lý request từ client để lấy thống kê của user
     * Format request: "GET_USER_STATS;username"
     * 
     * @param request Chuỗi request từ client
     * @return Chuỗi response gửi về client
     */
    public String handleUserStatsRequest(String request) {
        try {
            String[] parts = request.split(";");
            
            if (parts.length < 2) {
                return "failed;Missing username parameter";
            }
            
            String username = parts[1];
            return getUserStats(username);
            
        } catch (Exception e) {
            System.err.println("❌ Error handling user stats request: " + e.getMessage());
            e.printStackTrace();
            return "failed;Error processing request";
        }
    }
}
