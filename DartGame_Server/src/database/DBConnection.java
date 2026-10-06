package database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/dart_socket_game";
    private static final String USER = "root";   
    private static final String PASSWORD = "12345678";  

    private static DBConnection instance;
    private Connection connection;

    private DBConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to Database.");
            createTables();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTables() {
        try {
            String createUsers = "CREATE TABLE IF NOT EXISTS users ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(50) UNIQUE NOT NULL, "
                    + "password_hash VARCHAR(255) NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")";
                    
            String createMatches = "CREATE TABLE IF NOT EXISTS matches ("
                    + "id VARCHAR(255) PRIMARY KEY, "
                    + "mode VARCHAR(20) NOT NULL, "
                    + "player1_id BIGINT NOT NULL, "
                    + "player2_id BIGINT, "
                    + "player2_type VARCHAR(20) NOT NULL, "
                    + "winner_id BIGINT, "
                    + "winner_type VARCHAR(20), "
                    + "end_reason VARCHAR(50), "
                    + "started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "ended_at TIMESTAMP, "
                    + "FOREIGN KEY (player1_id) REFERENCES users(id), "
                    + "FOREIGN KEY (player2_id) REFERENCES users(id) ON DELETE SET NULL"
                    + ")";

            String createMatchThrows = "CREATE TABLE IF NOT EXISTS match_throws ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "match_id VARCHAR(255) NOT NULL, "
                    + "player_id BIGINT, "
                    + "actor_type VARCHAR(20) NOT NULL, "
                    + "turn_number BIGINT NOT NULL, "
                    + "dart_number INT NOT NULL, "
                    + "x DOUBLE NOT NULL, "
                    + "y DOUBLE NOT NULL, "
                    + "rotation_degree INT NOT NULL, "
                    + "hit_area VARCHAR(50) NOT NULL, "
                    + "dart_score INT NOT NULL, "
                    + "remaining_score_after INT NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (match_id) REFERENCES matches(id), "
                    + "FOREIGN KEY (player_id) REFERENCES users(id) ON DELETE SET NULL"
                    + ")";

            java.sql.Statement stmt = connection.createStatement();
            stmt.execute(createUsers);
            stmt.execute(createMatches);
            stmt.execute(createMatchThrows);
            System.out.println("Tables checked/created successfully.");
        } catch (Exception e) {
            System.out.println("Failed to create tables: " + e.getMessage());
        }
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}