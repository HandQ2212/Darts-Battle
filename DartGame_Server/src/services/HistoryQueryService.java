package services;

import database.DBConnection;
import protocol.Message;
import protocol.MessageType;
import protocol.dto.HistoryMessages.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.*;

/** Database reads never block the game reader or the room lock. */
public final class HistoryQueryService {
    private static final ThreadPoolExecutor QUERIES=new ThreadPoolExecutor(2,4,30,TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(100),r->{Thread t=new Thread(r,"history-query");t.setDaemon(true);return t;});
    private static final String FIELDS="m.id,m.mode,m.player1_id,u1.username AS name1,m.player2_id,u2.username AS name2,"+
        "m.winner_id,m.winner_type,m.end_reason,m.ended_at,m.player1_final_score,m.player2_final_score,m.bot_difficulty";
    private static final String FROM=" FROM matches m LEFT JOIN users u1 ON u1.id=m.player1_id LEFT JOIN users u2 ON u2.id=m.player2_id ";
    private HistoryQueryService() {}
    public static void submit(Client client,Message message) {
        if(!client.supportsSessions())throw new IllegalArgumentException("Client cần cập nhật.");
        long viewer=client.getLoggedInUserId();
        try {QUERIES.execute(()->{
            try {
                HistoryWriter.drain();
                switch(message.getType()){
                    case RANKING_V2_REQUEST -> client.send(Message.of(MessageType.RANKING_V2_RESULT,rankings()));
                    case PLAYER_MATCHES_REQUEST -> client.send(Message.of(MessageType.PLAYER_MATCHES_RESULT,page(viewer,(PageRequest)message.getPayload())));
                    case MATCH_DETAIL_REQUEST -> client.send(Message.of(MessageType.MATCH_DETAIL_RESULT,detail(viewer,(DetailRequest)message.getPayload())));
                    default -> throw new IllegalArgumentException("Yêu cầu không hợp lệ.");
                }
            } catch(Exception e){respondError(client,message,"Không tải được dữ liệu. Vui lòng thử lại.");}
        });} catch(RejectedExecutionException e){respondError(client,message,"Server đang bận. Vui lòng thử lại.");}
    }
    private static void respondError(Client c,Message m,String error){
        switch(m.getType()){
            case RANKING_V2_REQUEST -> c.send(Message.of(MessageType.RANKING_V2_RESULT,new Rankings(new ArrayList<>(),error)));
            case PLAYER_MATCHES_REQUEST -> {PageRequest req=m.getPayload() instanceof PageRequest p?p:null;c.send(Message.of(MessageType.PLAYER_MATCHES_RESULT,new Page(req==null?-1:req.playerId(),req==null?0:req.page(),false,new ArrayList<>(),error)));}
            case MATCH_DETAIL_REQUEST -> c.send(Message.of(MessageType.MATCH_DETAIL_RESULT,new Detail(null,new ArrayList<>(),error)));
            default -> {}
        }
    }
    private static Rankings rankings() throws SQLException {
        ArrayList<Rank> rows=new ArrayList<>();
        String sql="SELECT u.id,u.username,COUNT(m.id) AS wins FROM users u LEFT JOIN matches m ON u.id=m.winner_id AND m.mode='PVP' AND m.ended_at IS NOT NULL GROUP BY u.id,u.username ORDER BY wins DESC,u.username ASC LIMIT 100";
        try(Connection c=DBConnection.getInstance().getConnection();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){
            while(r.next())rows.add(new Rank(r.getLong(1),r.getString(2),r.getInt(3),rows.size()+1));
        }
        return new Rankings(rows,null);
    }
    private static Page page(long viewer,PageRequest req)throws SQLException{
        if(req==null||req.playerId()<1||req.page()<0||req.page()>100000)throw new IllegalArgumentException();
        boolean own=viewer==req.playerId()&&!req.publicOnly();
        String sql="SELECT "+FIELDS+FROM+"WHERE (m.player1_id=? OR m.player2_id=?) AND m.ended_at IS NOT NULL "+(own?"":"AND m.mode='PVP' ")+"ORDER BY m.ended_at DESC,m.id DESC LIMIT 26 OFFSET ?";
        ArrayList<Match> rows=new ArrayList<>();
        try(Connection c=DBConnection.getInstance().getConnection();PreparedStatement s=c.prepareStatement(sql)){
            s.setLong(1,req.playerId());s.setLong(2,req.playerId());s.setInt(3,req.page()*25);
            try(ResultSet r=s.executeQuery()){while(r.next())rows.add(match(r));}
        }
        boolean more=rows.size()>25;if(more)rows.remove(25);return new Page(req.playerId(),req.page(),more,rows,null);
    }
    private static Long nullableLong(ResultSet r,String col)throws SQLException{long n=r.getLong(col);return r.wasNull()?null:n;}
    private static Integer nullableInt(ResultSet r,String col)throws SQLException{int n=r.getInt(col);return r.wasNull()?null:n;}
    private static Match match(ResultSet r)throws SQLException {
        Integer a=nullableInt(r,"player1_final_score"),b=nullableInt(r,"player2_final_score");
        return new Match(r.getString("id"),r.getString("mode"),r.getLong("player1_id"),r.getString("name1"),
            nullableLong(r,"player2_id"),r.getString("name2"),nullableLong(r,"winner_id"),r.getString("winner_type"),
            r.getString("end_reason"),r.getString("ended_at"),a,b,r.getString("bot_difficulty"),false,a==null||b==null);
    }
    private static Detail detail(long viewer,DetailRequest req)throws SQLException{
        if(req==null||req.matchId()==null||req.matchId().length()>255)throw new IllegalArgumentException();
        try(Connection c=DBConnection.getInstance().getConnection()){
            Match m;
            try(PreparedStatement s=c.prepareStatement("SELECT "+FIELDS+FROM+"WHERE m.id=? AND m.ended_at IS NOT NULL")){
                s.setString(1,req.matchId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException();m=match(r);}
            }
            if(!"PVP".equals(m.mode())&&viewer!=m.player1Id()&&!java.util.Objects.equals(viewer,m.player2Id()))throw new IllegalArgumentException();
            ArrayList<Dart> darts=new ArrayList<>();int[] previous={301,301};boolean[] reliable={true,true};
            long lastTurn=0;int lastIndex=0;boolean old=false;
            try(PreparedStatement s=c.prepareStatement("SELECT turn_number,dart_number,player_id,actor_type,dart_score,remaining_score_after,is_bust FROM match_throws WHERE match_id=? ORDER BY turn_number,dart_number")){
                s.setString(1,req.matchId());try(ResultSet r=s.executeQuery()){
                    while(r.next()){
                        long turn=r.getLong(1);int index=r.getInt(2);Long id=nullableLong(r,"player_id");String actor=r.getString(4);
                        int side=java.util.Objects.equals(id,m.player1Id())?0:1,score=r.getInt(5),remaining=r.getInt(6);
                        if(turn<1||index<1||index>3||(lastTurn==turn&&index!=lastIndex+1)||(lastTurn!=turn&&(index!=1||turn!=lastTurn+1)))reliable[0]=reliable[1]=false;
                        boolean storedBust=r.getBoolean(7);Boolean bust=r.wasNull()?null:storedBust;
                        if(bust==null){old=true;if(reliable[side]){boolean inferred=score>previous[side];if((inferred?previous[side]:previous[side]-score)==remaining)bust=inferred;else reliable[side]=false;}}
                        if(bust!=null&&((bust?previous[side]:previous[side]-score)!=remaining))reliable[side]=false;
                        previous[side]=remaining;lastTurn=turn;lastIndex=index;
                        darts.add(new Dart(turn,index,id,actor,score,remaining,bust));
                    }
                }
            }
            Integer a=m.score1(),b=m.score2();boolean reconstructed=false;
            if(a==null&&reliable[0]&&!darts.isEmpty()){a=previous[0];reconstructed=true;}
            if(b==null&&reliable[1]&&!darts.isEmpty()){b=previous[1];reconstructed=true;}
            m=new Match(m.matchId(),m.mode(),m.player1Id(),m.player1Name(),m.player2Id(),m.player2Name(),m.winnerId(),m.winnerType(),m.reason(),m.endedAt(),a,b,m.difficulty(),reconstructed||old,a==null||b==null||!reliable[0]||!reliable[1]);
            return new Detail(m,darts,null);
        }
    }
}
