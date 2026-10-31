import database.DBConnection;
import java.sql.*;

/** Run against an isolated restored database, never against the live database. */
public class MigrationIntegrationTest {
    private static long count(Connection c,String table)throws SQLException{
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}
    }
    private static void column(Connection c,String table,String name)throws SQLException{
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM "+table+" WHERE 1=0")){
            for(int i=1;i<=r.getMetaData().getColumnCount();i++)if(name.equalsIgnoreCase(r.getMetaData().getColumnName(i)))return;
            throw new AssertionError("Missing column "+table+"."+name);
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=1||!args[0].matches("darts_migration_check_[0-9]+"))throw new IllegalArgumentException("Only a named isolated migration copy may be used");
        System.setProperty("darts.db.url","jdbc:mysql://127.0.0.1:3307/"+args[0]);System.setProperty("darts.db.user","root");System.setProperty("darts.db.password","");
        long u,m,t;try(Connection c=DBConnection.getInstance().getConnection()){u=count(c,"users");m=count(c,"matches");t=count(c,"match_throws");}
        DBConnection.getInstance().initialize();DBConnection.getInstance().initialize();
        try(Connection c=DBConnection.getInstance().getConnection()){
            if(u!=count(c,"users")||m!=count(c,"matches")||t!=count(c,"match_throws"))throw new AssertionError("Migration changed row counts");
            for(String name:new String[]{"session_id","round_index","bot_difficulty","player1_final_score","player2_final_score"})column(c,"matches",name);
            column(c,"match_throws","is_bust");
        }
        System.out.println("PASS restored MySQL database migrated twice without losing rows");
    }
}
