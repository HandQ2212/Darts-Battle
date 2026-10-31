import database.DBConnection;
import models.Room;
import protocol.MessageType;
import protocol.dto.GameMessages.*;
import protocol.dto.ThrowCoordinateRequest;
import protocol.dto.MediaMessages.*;
import protocol.dto.SessionMessages.*;
import protocol.dto.HistoryMessages.*;
import services.*;
import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Two upgraded real socket clients, plus an outsider, using an isolated database. */
public class SessionIntegrationTest extends MultiplayerIntegrationTest {
    public static void main(String[] args)throws Exception{
        System.setProperty("darts.db.url","jdbc:h2:mem:session;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        System.setProperty("darts.db.user","sa");System.setProperty("darts.db.password","");System.setProperty("darts.rotationDelayMillis","25");
        DBConnection.getInstance().initialize();DBConnection.getInstance().initialize();
        server=new ServerSocket(0,50,InetAddress.getLoopbackAddress());
        Thread accept=new Thread(()->{try{while(!server.isClosed())new Client(server.accept()).start();}catch(IOException ignored){}});accept.setDaemon(true);accept.start();
        try{
            Peer a=new Peer(),b=new Peer(),c=new Peer();a.registerLogin("Alice");b.registerLogin("Bobby");c.registerLogin("Carol");
            a.send(MessageType.FEATURE_SUPPORT,null);b.send(MessageType.FEATURE_SUPPORT,null);c.send(MessageType.FEATURE_SUPPORT,null);
            MatchSnapshot first=awaitRotation(a,b,match(a,b));String session=first.matchId();
            a.send(MessageType.MEDIA_JOIN,new JoinRequest(session));IceConfig ice=(IceConfig)a.take(MessageType.MEDIA_ICE_SERVERS).getPayload();
            b.send(MessageType.MEDIA_JOIN,new JoinRequest(session));IceConfig ice2=(IceConfig)b.take(MessageType.MEDIA_ICE_SERVERS).getPayload();
            check(ice.callId().equals(ice2.callId()),"same call established");
            Room room=RoomManager.getInstance().getRoom(session);
            synchronized(room){room.getGameState().setPlayer1RemainingScore(1);}
            BotAim.Target target=BotAim.optimal(1,first.rotationDegree());
            hit(a,b,first,target.x(),target.y());a.take(MessageType.MATCH_FINISHED);b.take(MessageType.MATCH_FINISHED);
            check("RESULTS".equals(room.getSessionPhase())&&RoomManager.getInstance().findRoomOfUser(a.userId)==room,"results keep session and players busy");
            a.send(MessageType.SESSION_READY,new ReadyRequest(session,session,"a-ready",true));
            State waiting=(State)a.take(MessageType.SESSION_STATUS,m->((State)m.getPayload()).player1Ready()).getPayload();
            check(waiting.player1Ready()&&!waiting.player2Ready(),"first player waits");
            a.send(MessageType.SESSION_READY,new ReadyRequest(session,session,"a-cancel",false));
            a.take(MessageType.SESSION_STATUS,m->!((State)m.getPayload()).player1Ready());check(true,"readiness can be cancelled");
            c.send(MessageType.SESSION_READY,new ReadyRequest(session,session,"outside",true));c.take(MessageType.ERROR);
            ExecutorService workers=Executors.newFixedThreadPool(2);
            try{
                Future<?> x=workers.submit(()->{try{a.send(MessageType.SESSION_READY,new ReadyRequest(session,session,"a-again",true));}catch(Exception e){throw new RuntimeException(e);}});
                Future<?> y=workers.submit(()->{try{b.send(MessageType.SESSION_READY,new ReadyRequest(session,session,"b-ready",true));}catch(Exception e){throw new RuntimeException(e);}});x.get();y.get();
            }finally{workers.shutdownNow();}
            MatchSnapshot second=a.snapshot(MessageType.MATCH_STARTED),other=b.snapshot(MessageType.MATCH_STARTED);
            check(second.matchId().equals(other.matchId())&&!session.equals(second.matchId())&&second.actorId()==b.userId&&second.player1Score()==301,"new match once and alternate first player");
            a.send(MessageType.MEDIA_DESCRIPTION,new Signal(session,ice.callId(),"OFFER","v=0\r\n",null,0,0));
            Signal forwarded=(Signal)b.take(MessageType.MEDIA_DESCRIPTION).getPayload();
            check(forwarded.callId().equals(ice.callId()),"same media call survives match transition");
            a.send(MessageType.CHAT_MESSAGE,new ChatMessage(session,"still here",null));ChatMessage chat=(ChatMessage)b.take(MessageType.CHAT_MESSAGE).getPayload();
            check("still here".equals(chat.text()),"chat uses stable session id");
            a.send(MessageType.THROW_COORDINATE,new ThrowCoordinateRequest(session,first.turnId(),1,"old",0,0));a.take(MessageType.ERROR);check(true,"old match command rejected");
            synchronized(room){room.deadline=0;}GameService.getInstance().expireTurns();
            a.take(MessageType.MATCH_FINISHED);b.take(MessageType.MATCH_FINISHED);
            check("RESULTS".equals(room.getSessionPhase()),"timeout keeps the session open");
            a.send(MessageType.SESSION_READY,new ReadyRequest(session,second.matchId(),"a-third",true));
            a.send(MessageType.SESSION_READY,new ReadyRequest(session,second.matchId(),"a-third",true));
            b.send(MessageType.SESSION_READY,new ReadyRequest(session,second.matchId(),"b-third",true));
            MatchSnapshot third=a.snapshot(MessageType.MATCH_STARTED);b.take(MessageType.MATCH_STARTED);
            check(third.actorId()==a.userId&&room.getRoundIndex()==3,"repeated readiness creates one match and alternates again");
            synchronized(room){room.deadline=0;}GameService.getInstance().expireTurns();
            a.take(MessageType.MATCH_FINISHED);b.take(MessageType.MATCH_FINISHED);
            a.send(MessageType.SESSION_READY,new ReadyRequest(session,third.matchId(),"a-wait",true));
            a.take(MessageType.SESSION_STATUS,m->{State s=(State)m.getPayload();return s.matchId().equals(third.matchId())&&s.player1Ready();});
            b.send(MessageType.SESSION_LEAVE,new LeaveRequest(session,"leave"));
            a.take(MessageType.SESSION_STATUS,m->"CLOSED".equals(((State)m.getPayload()).phase()));b.take(MessageType.SESSION_STATUS,m->"CLOSED".equals(((State)m.getPayload()).phase()));
            check(RoomManager.getInstance().getSession(session)==null,"leaving while waiting closes entire session");
            HistoryWriter.drain();
            try(Connection db=DBConnection.getInstance().getConnection();PreparedStatement s=db.prepareStatement("SELECT COUNT(*) FROM matches WHERE session_id=? AND ended_at IS NOT NULL")){
                s.setString(1,session);try(ResultSet rows=s.executeQuery()){rows.next();check(rows.getInt(1)==3,"every round is persisted exactly once");}
            }
            c.send(MessageType.RANKING_V2_REQUEST,null);Rankings ranks=(Rankings)c.take(MessageType.RANKING_V2_RESULT).getPayload();
            check(ranks.error()==null&&ranks.rows().stream().anyMatch(r->r.userId()==a.userId),"rankings return real user IDs");
            c.send(MessageType.PLAYER_MATCHES_REQUEST,new PageRequest(a.userId,0,true));Page page=(Page)c.take(MessageType.PLAYER_MATCHES_RESULT).getPayload();
            check(page.error()==null&&page.matches().stream().anyMatch(m->m.matchId().equals(session)),"public PvP history includes correct match");
            c.send(MessageType.MATCH_DETAIL_REQUEST,new DetailRequest(session));Detail detail=(Detail)c.take(MessageType.MATCH_DETAIL_RESULT).getPayload();
            check(detail.error()==null&&detail.match().matchId().equals(session)&&detail.darts().size()==1,"public match detail includes throws");
            try(Connection db=DBConnection.getInstance().getConnection()){
                try(PreparedStatement s=db.prepareStatement("INSERT INTO matches(id,mode,player1_id,player2_id,player2_type,winner_id,winner_type,end_reason,ended_at) VALUES('legacy','PVP',?,?,'HUMAN',?,'HUMAN','FORFEIT',CURRENT_TIMESTAMP)")){
                    s.setLong(1,a.userId);s.setLong(2,b.userId);s.setLong(3,a.userId);s.executeUpdate();
                }
                String sql="INSERT INTO match_throws(match_id,player_id,actor_type,turn_number,dart_number,x,y,rotation_degree,hit_area,dart_score,remaining_score_after) VALUES('legacy',?,'HUMAN',?,?,0,0,0,'SINGLE',?,?)";
                try(PreparedStatement s=db.prepareStatement(sql)){
                    int[][] rows={{1,1,60,241},{1,2,60,181},{1,3,60,121},{2,1,0,301},{2,2,0,301},{2,3,0,301},{3,1,60,61},{3,2,60,1},{3,3,50,1}};
                    for(int[] row:rows){s.setLong(1,row[0]==2?b.userId:a.userId);s.setInt(2,row[0]);s.setInt(3,row[1]);s.setInt(4,row[2]);s.setInt(5,row[3]);s.executeUpdate();}
                }
            }
            DBConnection.getInstance().initialize();
            c.send(MessageType.MATCH_DETAIL_REQUEST,new DetailRequest("legacy"));Detail legacy=(Detail)c.take(MessageType.MATCH_DETAIL_RESULT).getPayload();
            check(legacy.error()==null&&legacy.match().reconstructed()&&!legacy.match().incomplete()&&legacy.match().score1()==1&&legacy.darts().get(8).bust(),"old complete throws reconstruct final score and bust");
            a.send(MessageType.CREATE_BOT_MATCH,new protocol.dto.BotMatchRequest(enums.BotDifficulty.HARD));MatchSnapshot bot=a.snapshot(MessageType.MATCH_STARTED);
            a.send(MessageType.SESSION_LEAVE,new LeaveRequest(bot.matchId(),"leave-bot"));a.take(MessageType.MATCH_FINISHED);
            c.send(MessageType.MATCH_DETAIL_REQUEST,new DetailRequest(bot.matchId()));Detail denied=(Detail)c.take(MessageType.MATCH_DETAIL_RESULT).getPayload();
            check(denied.error()!=null&&denied.match()==null,"other players cannot access bot details");
            a.send(MessageType.PLAYER_MATCHES_REQUEST,new PageRequest(a.userId,0,true));Page publicPage=(Page)a.take(MessageType.PLAYER_MATCHES_RESULT).getPayload();
            a.send(MessageType.PLAYER_MATCHES_REQUEST,new PageRequest(a.userId,0,false));Page personal=(Page)a.take(MessageType.PLAYER_MATCHES_RESULT).getPayload();
            check(publicPage.matches().stream().noneMatch(m->m.matchId().equals(bot.matchId()))&&personal.matches().stream().anyMatch(m->m.matchId().equals(bot.matchId())),"rankings hide bot matches even for the owner");
            System.out.println("ALL SESSION AND HISTORY CHECKS PASSED");
        }finally{for(Peer p:peers)try{p.close();}catch(Exception ignored){}server.close();}
    }
}
