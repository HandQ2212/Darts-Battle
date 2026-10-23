package database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static final DBConnection INSTANCE = new DBConnection();
    private DBConnection() {}
    private static String setting(String key, String env, String fallback) {
        return System.getProperty(key, System.getenv().getOrDefault(env, fallback));
    }
    public Connection getConnection() throws java.sql.SQLException {
        return DriverManager.getConnection(setting("darts.db.url", "DARTS_DB_URL",
                "jdbc:mysql://localhost:3306/dart_socket_game?connectTimeout=5000&socketTimeout=5000"),
                setting("darts.db.user", "DARTS_DB_USER", "root"),
                setting("darts.db.password", "DARTS_DB_PASSWORD", ""));
    }
    public static DBConnection getInstance() { return INSTANCE; }
    public void initialize() { createTables(); }

    private void createTables() {
        try (Connection connection = getConnection()) {
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

            try (java.sql.Statement stmt = connection.createStatement()) {
                stmt.execute(createUsers);
                stmt.execute(createMatches);
                stmt.execute(createMatchThrows);
                // Upgrade existing databases too; never delete duplicate historical rows silently.
                boolean unique=false;
                java.util.Map<String,java.util.Set<String>> indexColumns=new java.util.HashMap<>();
                try (java.sql.ResultSet indexes=connection.getMetaData().getIndexInfo(null,null,"match_throws",true,false)) {
                    while(indexes.next()) {
                        String name=indexes.getString("INDEX_NAME"), column=indexes.getString("COLUMN_NAME");
                        if(name!=null && column!=null) indexColumns.computeIfAbsent(name,k->new java.util.HashSet<>()).add(column.toLowerCase(java.util.Locale.ROOT));
                    }
                }
                unique=indexColumns.values().stream().anyMatch(columns->columns.equals(java.util.Set.of("match_id","turn_number","dart_number")));
                if(!unique) stmt.execute("ALTER TABLE match_throws ADD CONSTRAINT uq_match_dart UNIQUE(match_id,turn_number,dart_number)");
            }
            System.out.println("Tables checked/created successfully.");
        } catch (Exception e) {
            throw new IllegalStateException("Không khởi tạo được DB; kiểm tra DARTS_DB_URL/USER/PASSWORD", e);
        }
    }
}
