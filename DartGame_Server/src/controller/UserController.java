/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import java.sql.Connection;
import database.DBConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
/**
 *
 * @author kaita
 */
public class UserController {
    // 1 controller này quản lý 1 người dùng thôi
    
    //  SQL
    private final String INSERT_USER = "INSERT INTO users (username, password, score) VALUES (?, ?, 0)";
    
    private final String CHECK_USER = "SELECT id from users WHERE username = ? limit 1";
    
    private final String LOGIN_USER = "SELECT username, password FROM users WHERE username=? AND password=?";
    
    private final String GET_INFO_USER = "SELECT username, password FROM users WHERE username=?";

     // MATCH HISTORY SQL
    private final String INSERT_HISTORY =
        "INSERT INTO match_history (username, opponent, result, match_time) " +
        "VALUES (?, ?, ?, ?)";

    //  Instance
    private final Connection con;
    
    public UserController() {
        this.con = DBConnection.getInstance().getConnection();
    }
    
    public String register(String username, String password) {
    	//  Check user exit
        try {
            PreparedStatement p = con.prepareStatement(CHECK_USER);
            p.setString(1, username);
            ResultSet r = p.executeQuery();
            if (r.next()) {
                System.out.println("Register failed");
                return "failed;" + "User Already Exit";
            } else {
                r.close();
                p.close();
                p = con.prepareStatement(INSERT_USER);
                System.out.println("Register Successful");
                p.setString(1, username);
                p.setString(2, password);
                p.executeUpdate();
                p.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "success;";
    }
  
    public String login(String username, String password) {
    	//  Check user exit
        try {
            PreparedStatement p = con.prepareStatement(LOGIN_USER);
            //  Login User 
            p.setString(1, username);
            p.setString(2, password);
            ResultSet r = p.executeQuery();
//            System.out.println("r: " + r.next());

            if (r.next()) {
                return "success;" + username;
            } else {
                return "failed;" + "Please enter the correct account password!";
            }
        } catch (SQLException e) {
            
        }
        return null;
    }
    
    public String updateUser(String username, String newPassword, Integer newScore) {
        // Chỉ cập nhật các trường được truyền vào (không null)
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        boolean hasSet = false;

        if (newPassword != null) {
            sql.append("password = ?");
            hasSet = true;
        }

        if (newScore != null) {
            if (hasSet) sql.append(", ");
            sql.append("score = ?");
            hasSet = true;
        }

        // Nếu không có trường nào để update
        if (!hasSet) {
            return "failed;No fields to update";
        }

        sql.append(" WHERE username = ?");

        try {
            PreparedStatement p = con.prepareStatement(sql.toString());
            int index = 1;

            if (newPassword != null) {
                p.setString(index++, newPassword);
            }
            if (newScore != null) {
                p.setInt(index++, newScore);
            }
            p.setString(index, username);

            int rows = p.executeUpdate();
            p.close();

            if (rows > 0) {
                return "success;User updated successfully";
            } else {
                return "failed;User not found";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "failed;Database error: " + e.getMessage();
        }
    }
    
    public String increaseScore(String username) {
        String sql = "UPDATE users SET score = score + 1 WHERE username = ?";

        try {
            PreparedStatement p = con.prepareStatement(sql);
            p.setString(1, username);
            int rows = p.executeUpdate();
            p.close();

            if (rows > 0) {
                return "success;Score increased by 1";
            } else {
                return "failed;User not found";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "failed;Database error: " + e.getMessage();
        }
    }

    public void saveMatchHistory(String username, String opponent, String result) {
        try {
            PreparedStatement p = con.prepareStatement(INSERT_HISTORY);

            p.setString(1, username);
            p.setString(2, opponent);
            p.setString(3, result);
            p.setObject(4, LocalDateTime.now());

            p.executeUpdate();
            p.close();

        } catch (SQLException e) {
            System.out.println("Error saving match history: " + e.getMessage());
        }
    }
    public String getMatchHistory(String username) {
        String sql = "SELECT opponent, result, match_time FROM match_history WHERE username=? ORDER BY match_time DESC";
        
        StringBuilder sb = new StringBuilder();
        int count = 0;

        try {
            PreparedStatement p = con.prepareStatement(sql);
            p.setString(1, username);
            ResultSet rs = p.executeQuery();

            while (rs.next()) {
                String opponent = rs.getString("opponent");
                String result = rs.getString("result");
                String time = rs.getString("match_time");

                // mỗi trận: username|opponent|result|time
                sb.append(username).append("|")
                .append(opponent).append("|")
                .append(result).append("|")
                .append(time).append(";");

                count++;
            }

            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1); // bỏ dấu ; cuối
            }

        } catch (SQLException e) {
            return "failed;Database error";
        }

        return "success;" + count + ";" + sb.toString();
    }
}
