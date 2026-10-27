import config.DartboardLayout;
import database.DBConnection;
import dartgame_server.DartGame_Server;
import enums.*;
import models.Room;
import protocol.*;
import protocol.dto.*;
import protocol.dto.GameMessages.*;
import services.*;
import network.*;
import javax.net.ssl.SSLContext;
import java.nio.file.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/** Real TCP, real controllers and JDBC; H2 in MySQL mode isolates the user's database. */
public class MultiplayerIntegrationTest {
    static int checks;
    static ServerSocket server;
    static WebSocketBridge websocketBridge;
    static SSLContext pinned;
    static final List<Peer> peers=new ArrayList<>();
    static class Peer implements AutoCloseable {
        final Socket socket;
        final ObjectOutputStream out;
        final ObjectInputStream in;
        final BlockingQueue<Message> inbox=new LinkedBlockingQueue<>();
        final List<Message> seen=new CopyOnWriteArrayList<>();
        long userId;
        String username;
        Peer() throws Exception {
            if(websocketBridge==null)socket=new Socket("127.0.0.1",server.getLocalPort());
            else socket=TransportSecurity.secure(WebSocketSocket.open(URI.create("ws://127.0.0.1:"+websocketBridge.getPort()+"/play"),5000),"localhost",server.getLocalPort(),5000,pinned);
            socket.setSoTimeout(10000);
            out=new ObjectOutputStream(socket.getOutputStream());out.flush();
            in=new ObjectInputStream(socket.getInputStream());in.setObjectInputFilter(WireFilter.create());
            Thread reader=new Thread(()->{try{while(!socket.isClosed()){Message m=(Message)in.readObject();seen.add(m);inbox.add(m);}}catch(Exception ignored){}},"test-reader");
            reader.setDaemon(true);reader.start();peers.add(this);
        }
        synchronized void send(MessageType type,Object payload) throws Exception {out.writeObject(Message.of(type,payload));out.reset();out.flush();}
        Message take(MessageType type) throws Exception {return take(type,m->true);}
        Message take(MessageType type,Predicate<Message> predicate) throws Exception {
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(System.nanoTime()<end) {Message m=inbox.poll(100,TimeUnit.MILLISECONDS);if(m!=null&&m.getType()==type&&predicate.test(m))return m;}
            throw new AssertionError("Timeout waiting "+type+" for "+username+"; seen="+seen.stream().map(Message::getType).toList());
        }
        void registerLogin(String name) throws Exception {
            username=name;send(MessageType.REGISTER_REQUEST,new LoginRequest(name,"secret123"));
            AuthResponse registered=(AuthResponse)take(MessageType.REGISTER_RESULT).getPayload();check(registered.success(),"register "+name);
            send(MessageType.LOGIN_REQUEST,new LoginRequest(name,"secret123"));
            AuthResponse auth=(AuthResponse)take(MessageType.LOGIN_RESULT).getPayload();check(auth.success(),"login "+name);userId=auth.userId();
        }
        MatchSnapshot snapshot(MessageType type) throws Exception {return (MatchSnapshot)take(type).getPayload();}
        public void close() throws Exception {socket.close();}
    }
    static synchronized void check(boolean condition,String name) {if(!condition)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static void eventually(java.util.function.BooleanSupplier condition,String name) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(!condition.getAsBoolean()&&System.nanoTime()<until)Thread.sleep(20);
        check(condition.getAsBoolean(),name);
    }
    static MatchSnapshot match(Peer a,Peer b) throws Exception {
        a.send(MessageType.INVITE_REQUEST,b.username);
        InviteResponse invite=(InviteResponse)b.take(MessageType.INVITE_RECEIVED).getPayload();
        check(invite.senderId()==a.userId,"invitation identities");
        b.send(MessageType.INVITE_ACCEPT,invite.id());
        MatchSnapshot s=a.snapshot(MessageType.MATCH_STARTED),other=b.snapshot(MessageType.MATCH_STARTED);
        check(s.matchId().equals(other.matchId())&&s.actorId()==a.userId&&s.player1Score()==301,"shared initial snapshot");
        a.take(MessageType.TURN_PREPARING);b.take(MessageType.TURN_PREPARING);return s;
    }
    static MatchSnapshot awaitRotation(Peer a,Peer b,MatchSnapshot s) throws Exception {
        MatchSnapshot applied=a.snapshot(MessageType.ROTATION_APPLIED);a.take(MessageType.TURN_STARTED);
        if(b!=null){check(applied.equals(b.snapshot(MessageType.ROTATION_APPLIED)),"both peers share automatic rotation");b.take(MessageType.TURN_STARTED);}
        check(applied.rotationDegree()==s.rotationDegree()&&applied.rotationDegree()>=0&&applied.rotationDegree()<360&&applied.status()==MatchStatus.PLAYING,"server automatically chooses and applies rotation");return applied;
    }
    static ThrowResolvedResponse hit(Peer a,Peer b,MatchSnapshot s,double x,double y) throws Exception {
        ThrowCoordinateRequest req=new ThrowCoordinateRequest(s.matchId(),s.turnId(),s.dartsThrown()+1,UUID.randomUUID().toString(),x,y);
        a.send(MessageType.THROW_COORDINATE,req);
        ThrowResolvedResponse res=(ThrowResolvedResponse)a.take(MessageType.THROW_RESOLVED).getPayload();
        if(b!=null){ThrowResolvedResponse other=(ThrowResolvedResponse)b.take(MessageType.THROW_RESOLVED).getPayload();check(res.getSnapshot().equals(other.getSnapshot()),"matching throw snapshots");}
        return res;
    }
    static void scorer() {
        DartboardScorer scorer=new DartboardScorer();
        check(scorer.calculateDartScore(0,0,0)==50,"bull scores 50");
        for(int rotation:new int[]{0,90,180,270})for(int i=0;i<20;i++) {
            double a=Math.toRadians(i*18-90+rotation);
            for(double r:new double[]{40,90,150}) {
                int multiplier=r==90?3:r==150?2:1;
                check(scorer.calculateDartScore(r*Math.cos(a),r*Math.sin(a),rotation)==DartboardLayout.BOARD_ORDER[i]*multiplier,"sector "+i+" rotation "+rotation+" radius "+r);
            }
        }
        check(scorer.calculateDartScore(161,0,0)==0,"outside is miss");
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("darts.db.url","jdbc:h2:mem:darts;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        System.setProperty("darts.db.user","sa");System.setProperty("darts.db.password","");System.setProperty("darts.botDelayMillis","25");System.setProperty("darts.rotationDelayMillis","25");
        DBConnection.getInstance().initialize();DBConnection.getInstance().initialize();
        if(Boolean.getBoolean("darts.test.websocket")) {
            pinned=TransportSecurity.pinnedContext(Files.newInputStream(Path.of(System.getenv("DARTS_TEST_TLS_CERT"))));
            server=TransportSecurity.serverContext(Path.of(System.getenv("DARTS_TEST_TLS_STORE")),System.getenv("DARTS_TEST_TLS_PASSWORD").toCharArray()).getServerSocketFactory().createServerSocket(0,50,InetAddress.getLoopbackAddress());
            websocketBridge=new WebSocketBridge(new InetSocketAddress("127.0.0.1",0),server.getLocalPort());websocketBridge.start();websocketBridge.awaitStarted();
            System.out.println("RUNNING FULL MULTIPLAYER SUITE OVER WEBSOCKET + TLS");
        } else server=new ServerSocket(0,50,InetAddress.getLoopbackAddress());
        Thread accept=new Thread(()->{try{while(!server.isClosed())new Client(server.accept()).start();}catch(IOException ignored){}},"test-accept");accept.setDaemon(true);accept.start();
        try {
            scorer();
            Peer guest=new Peer();guest.send(MessageType.CREATE_BOT_MATCH,null);guest.take(MessageType.ERROR);check(RoomManager.getInstance().allRooms().isEmpty(),"guest rejected");
            Peer a=new Peer(),b=new Peer(),c=new Peer(),d=new Peer();
            a.registerLogin("Alice");b.registerLogin("Bobby");c.registerLogin("Carol");d.registerLogin("David");
            Peer duplicate=new Peer();duplicate.send(MessageType.LOGIN_REQUEST,new LoginRequest("Alice","secret123"));duplicate.take(MessageType.ERROR);duplicate.close();
            check(ClientManager.getInstance().getClient(a.userId)!=null,"duplicate login does not replace original");
            Peer wrong=new Peer();wrong.send(MessageType.LOGIN_REQUEST,new LoginRequest("Alice","badpass"));check(!((AuthResponse)wrong.take(MessageType.LOGIN_RESULT).getPayload()).success(),"wrong password rejected");
            a.send(MessageType.ONLINE_LIST,null);List<?> online=(List<?>)a.take(MessageType.ONLINE_LIST,m->((List<?>)m.getPayload()).size()==4).getPayload();check(online.size()==4,"online list over TCP");
            System.setProperty("darts.rotationDelayMillis","800");
            MatchSnapshot preparing=match(a,b);
            a.send(MessageType.THROW_COORDINATE,new ThrowCoordinateRequest(preparing.matchId(),preparing.turnId(),1,"during-spin",0,0));a.take(MessageType.ERROR);
            a.send(MessageType.ROTATION_SUBMIT,new RotationRequest(preparing.matchId(),preparing.turnId(),45,"manual-spin"));a.take(MessageType.ERROR);
            check(RoomManager.getInstance().getRoom(preparing.matchId()).getGameState().getDartsThrown()==0,"rotation gates early throws and rejects manual input");
            awaitRotation(a,b,preparing);
            a.send(MessageType.LEAVE_MATCH,preparing.matchId());a.take(MessageType.MATCH_FINISHED);b.take(MessageType.MATCH_FINISHED);
            MatchSnapshot cancelled=match(a,b);
            a.send(MessageType.LEAVE_MATCH,cancelled.matchId());a.take(MessageType.MATCH_FINISHED);b.take(MessageType.MATCH_FINISHED);
            Thread.sleep(900);
            check(RoomManager.getInstance().getRoom(cancelled.matchId())==null,"leaving during rotation cannot restart a match");
            System.setProperty("darts.rotationDelayMillis","25");
            MatchSnapshot ab=match(a,b),cd=match(c,d);ab=awaitRotation(a,b,ab);cd=awaitRotation(c,d,cd);
            a.send(MessageType.CREATE_BOT_MATCH,null);a.take(MessageType.ERROR);check(RoomManager.getInstance().allRooms().size()==2,"one room per user");
            c.send(MessageType.THROW_COORDINATE,new ThrowCoordinateRequest(ab.matchId(),ab.turnId(),1,"outsider",0,0));c.take(MessageType.ERROR);
            b.send(MessageType.THROW_COORDINATE,new ThrowCoordinateRequest(ab.matchId(),ab.turnId(),1,"wrongturn",0,0));b.take(MessageType.ERROR);
            a.send(MessageType.THROW_COORDINATE,new ThrowCoordinateRequest(ab.matchId(),999,1,"stale",0,0));a.take(MessageType.ERROR);
            a.send(MessageType.THROW_COORDINATE,new ThrowCoordinateRequest(ab.matchId(),ab.turnId(),1,"nan",Double.NaN,0));a.take(MessageType.ERROR);
            check(RoomManager.getInstance().getRoom(ab.matchId()).getGameState().getDartsThrown()==0,"invalid commands never mutate game");
            a.send(MessageType.THROW_COORDINATE,"old string payload");a.take(MessageType.ERROR);a.send(MessageType.PING,null);a.take(MessageType.PONG);check(true,"malformed payload keeps session alive");
            ThrowCoordinateRequest original=new ThrowCoordinateRequest(ab.matchId(),ab.turnId(),1,"unique-throw",0,0);
            a.send(MessageType.THROW_COORDINATE,original);
            ThrowResolvedResponse first=(ThrowResolvedResponse)a.take(MessageType.THROW_RESOLVED).getPayload();b.take(MessageType.THROW_RESOLVED);
            check(first.getSnapshot().player1Score()==251&&first.getActorId()==a.userId,"correct identity and score");
            a.send(MessageType.THROW_COORDINATE,original);ThrowResolvedResponse replay=(ThrowResolvedResponse)a.take(MessageType.THROW_RESOLVED).getPayload();
            check(replay.getSnapshot().equals(first.getSnapshot())&&RoomManager.getInstance().getRoom(ab.matchId()).getGameState().getDartsThrown()==1,"retry idempotent");
            a.send(MessageType.ROTATION_SUBMIT,new RotationRequest(ab.matchId(),ab.turnId(),45,"mid-turn"));a.take(MessageType.ERROR);
            check(RoomManager.getInstance().getRoom(ab.matchId()).getGameState().getRotationDegree()==ab.rotationDegree(),"mid-turn rotation rejected");
            long bThrows=b.seen.stream().filter(m->m.getType()==MessageType.THROW_RESOLVED).count();
            a.send(MessageType.PING,null);a.take(MessageType.PONG);
            check(bThrows==1,"no duplicate opponent throw events");
            ab=hit(a,b,first.getSnapshot(),0,0).getSnapshot();ab=hit(a,b,ab,0,0).getSnapshot();
            MatchSnapshot next=a.snapshot(MessageType.TURN_PREPARING);b.take(MessageType.TURN_PREPARING);
            check(next.actorId()==b.userId&&next.turnId()==2&&next.dartsThrown()==0,"turn switches after three darts");
            check(RoomManager.getInstance().getRoom(cd.matchId()).getGameState().getPlayer1RemainingScore()==301,"other room isolated");
            a.send(MessageType.CHAT_MESSAGE,new ChatMessage(next.matchId(),"hello","FORGED"));ChatMessage chat=(ChatMessage)b.take(MessageType.CHAT_MESSAGE).getPayload();a.take(MessageType.CHAT_MESSAGE);
            check(chat.senderName().equals("Alice"),"chat sender authenticated");
            c.send(MessageType.CHAT_MESSAGE,new ChatMessage(next.matchId(),"intruder",null));c.take(MessageType.ERROR);
            a.send(MessageType.LEAVE_MATCH,next.matchId());MatchSnapshot ended=a.snapshot(MessageType.MATCH_FINISHED);b.take(MessageType.MATCH_FINISHED);
            check(ended.winnerId()==b.userId&&ended.endReason().equals("FORFEIT"),"leave forfeits once");
            check(RoomManager.getInstance().findRoomOfUser(a.userId)==null&&ClientManager.getInstance().getStatus(a.userId)==PlayerStatus.IDLE,"room released");
            c.close();MatchSnapshot disconnected=d.snapshot(MessageType.MATCH_FINISHED);
            check(disconnected.winnerId()==d.userId&&disconnected.endReason().equals("DISCONNECT"),"disconnect awards opponent");
            eventually(()->ClientManager.getInstance().getClient(c.userId)==null,"disconnect unregisters socket");
            // Bot completes all three throws and returns control.
            a.send(MessageType.CREATE_BOT_MATCH,null);MatchSnapshot bot=a.snapshot(MessageType.MATCH_STARTED);a.take(MessageType.TURN_PREPARING);bot=awaitRotation(a,null,bot);
            for(int i=0;i<3;i++)bot=hit(a,null,bot,300,300).getSnapshot();
            a.take(MessageType.TURN_PREPARING);a.take(MessageType.ROTATION_APPLIED);a.take(MessageType.TURN_STARTED);
            for(int i=0;i<3;i++){ThrowResolvedResponse br=(ThrowResolvedResponse)a.take(MessageType.THROW_RESOLVED).getPayload();check(br.getActorType()==ActorType.BOT&&br.getActorId()==null,"bot actor and throw "+i);}
            MatchSnapshot back=a.snapshot(MessageType.TURN_PREPARING);check(back.actorId()==a.userId&&back.turnId()==3,"bot returns human turn");
            a.send(MessageType.LEAVE_MATCH,back.matchId());a.take(MessageType.MATCH_FINISHED);
            // Natural win: six bulls = 300, then single 1. Alternate opponent misses.
            MatchSnapshot win=match(a,b);
            for(int round=0;round<2;round++) {
                win=awaitRotation(a,b,win);for(int i=0;i<3;i++)win=hit(a,b,win,0,0).getSnapshot();
                win=a.snapshot(MessageType.TURN_PREPARING);b.take(MessageType.TURN_PREPARING);win=awaitRotation(b,a,win);
                for(int i=0;i<3;i++)win=hit(b,a,win,300,300).getSnapshot();
                win=a.snapshot(MessageType.TURN_PREPARING);b.take(MessageType.TURN_PREPARING);
            }
            win=awaitRotation(a,b,win);
            ThrowResolvedResponse bust=hit(a,b,win,0,0);check(bust.isBust()&&bust.getRemainingScoreAfter()==1&&!bust.isTurnFinished(),"custom bust rule retained");
            double angle=Math.toRadians(-72+win.rotationDegree());ThrowResolvedResponse winning=hit(a,b,bust.getSnapshot(),50*Math.cos(angle),50*Math.sin(angle));
            check(winning.isMatchFinished()&&winning.getRemainingScoreAfter()==0,"exact zero wins");
            MatchSnapshot victory=a.snapshot(MessageType.MATCH_FINISHED);b.take(MessageType.MATCH_FINISHED);check(victory.winnerId()==a.userId,"winner ID broadcast");
            // Turn expiry and logout clean up too.
            MatchSnapshot timed=match(a,b);Room timedRoom=RoomManager.getInstance().getRoom(timed.matchId());synchronized(timedRoom){timedRoom.deadline=0;}GameService.getInstance().expireTurns();
            check(a.snapshot(MessageType.MATCH_FINISHED).endReason().equals("TURN_TIMEOUT"),"turn timeout finishes");b.take(MessageType.MATCH_FINISHED);
            MatchSnapshot logout=match(a,b);a.send(MessageType.LOGOUT_REQUEST,null);a.take(MessageType.LOGOUT_RESULT);check(b.snapshot(MessageType.MATCH_FINISHED).winnerId()==b.userId,"logout forfeits");
            // Rejection/expiry release both users; a third client cannot accept someone else's invitation.
            a.send(MessageType.LOGIN_REQUEST,new LoginRequest("Alice","secret123"));a.take(MessageType.LOGIN_RESULT);
            a.send(MessageType.INVITE_REQUEST,b.username);InviteResponse reject=(InviteResponse)b.take(MessageType.INVITE_RECEIVED).getPayload();
            d.send(MessageType.INVITE_ACCEPT,reject.id());d.take(MessageType.ERROR);
            b.send(MessageType.INVITE_REJECT,reject.id());b.take(MessageType.INVITE_RESULT);a.take(MessageType.INVITE_RESULT);
            check(ClientManager.getInstance().getStatus(a.userId)==PlayerStatus.IDLE&&ClientManager.getInstance().getStatus(b.userId)==PlayerStatus.IDLE,"rejected invitation releases both users");
            System.setProperty("darts.inviteTimeoutMillis","30");a.send(MessageType.INVITE_REQUEST,b.username);b.take(MessageType.INVITE_RECEIVED);Thread.sleep(60);LobbyService.getInstance().expire();
            a.take(MessageType.INVITE_RESULT);b.take(MessageType.INVITE_RESULT);check(ClientManager.getInstance().getStatus(a.userId)==PlayerStatus.IDLE,"expired invitation releases users");System.clearProperty("darts.inviteTimeoutMillis");
            Peer e=new Peer();e.registerLogin("Erin");
            MatchSnapshot concurrentAB=awaitRotation(a,b,match(a,b)),concurrentDE=awaitRotation(d,e,match(d,e));
            ExecutorService concurrent=Executors.newFixedThreadPool(2);
            try {
                Future<?> one=concurrent.submit(()->{try{MatchSnapshot s=concurrentAB;for(int i=0;i<3;i++)s=hit(a,b,s,0,0).getSnapshot();}catch(Exception ex){throw new RuntimeException(ex);}});
                Future<?> two=concurrent.submit(()->{try{MatchSnapshot s=concurrentDE;for(int i=0;i<3;i++)s=hit(d,e,s,300,300).getSnapshot();}catch(Exception ex){throw new RuntimeException(ex);}});
                one.get(10,TimeUnit.SECONDS);two.get(10,TimeUnit.SECONDS);
            } finally {concurrent.shutdownNow();}
            a.take(MessageType.TURN_PREPARING);b.take(MessageType.TURN_PREPARING);d.take(MessageType.TURN_PREPARING);e.take(MessageType.TURN_PREPARING);
            check(RoomManager.getInstance().getRoom(concurrentAB.matchId()).getGameState().getPlayer1RemainingScore()==151&&RoomManager.getInstance().getRoom(concurrentDE.matchId()).getGameState().getPlayer1RemainingScore()==301,"concurrent rooms have independent scores");
            a.send(MessageType.LEAVE_MATCH,concurrentAB.matchId());b.send(MessageType.LEAVE_MATCH,concurrentAB.matchId());a.take(MessageType.MATCH_FINISHED);b.take(MessageType.MATCH_FINISHED);
            a.send(MessageType.PING,null);b.send(MessageType.PING,null);a.take(MessageType.PONG);b.take(MessageType.PONG);
            check(a.seen.stream().filter(m->m.getType()==MessageType.MATCH_FINISHED&&((MatchSnapshot)m.getPayload()).matchId().equals(concurrentAB.matchId())).count()==1
                    &&b.seen.stream().filter(m->m.getType()==MessageType.MATCH_FINISHED&&((MatchSnapshot)m.getPayload()).matchId().equals(concurrentAB.matchId())).count()==1,"simultaneous leave finishes exactly once");
            d.send(MessageType.LEAVE_MATCH,concurrentDE.matchId());d.take(MessageType.MATCH_FINISHED);e.take(MessageType.MATCH_FINISHED);
            HistoryWriter.drain();
            try(Connection conn=DBConnection.getInstance().getConnection();Statement st=conn.createStatement()) {
                try(ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM match_throws WHERE player_id IS NULL AND actor_type='BOT'")){rs.next();check(rs.getInt(1)==3,"bot persisted with null player id");}
                try(ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM matches WHERE ended_at IS NULL")){rs.next();check(rs.getInt(1)==0,"all matches persisted as finished");}
                try(ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM match_throws WHERE match_id='"+next.matchId()+"'")){rs.next();check(rs.getInt(1)==3,"duplicate request persisted once");}
            }
            d.send(MessageType.GET_RANKING,100);d.take(MessageType.RANKING_RESULT);d.send(MessageType.GET_HISTORY,null);d.take(MessageType.HISTORY_RESULT);check(true,"ranking and history work after many DB operations");
            System.out.println("ALL "+checks+" CHECKS PASSED");
        } finally {for(Peer p:peers)try{p.close();}catch(Exception ignored){}server.close();if(websocketBridge!=null)websocketBridge.close();}
    }
}
