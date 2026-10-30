import database.DBConnection;
import enums.*;
import protocol.*;
import protocol.dto.*;
import protocol.dto.GameMessages.*;
import protocol.dto.MediaMessages.*;
import services.Client;
import ui.MediaCallSession;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Two WebRTC clients signal through a real game server with an isolated H2 database. */
public class MediaServerEndToEndTest {
    private static final CountDownLatch connected=new CountDownLatch(2),turnCredentials=new CountDownLatch(2);
    private static final AtomicReference<String> failure=new AtomicReference<>();
    private static final AtomicBoolean captured=new AtomicBoolean();
    private static final class Peer implements AutoCloseable {
        final Socket socket;
        final ObjectOutputStream out;
        final ObjectInputStream in;
        final BlockingQueue<Message> inbox=new LinkedBlockingQueue<>();
        final AtomicBoolean didConnect=new AtomicBoolean(),didReceiveTurn=new AtomicBoolean();
        volatile MediaCallSession media;
        Peer(int port)throws Exception {
            socket=new Socket(InetAddress.getLoopbackAddress(),port);
            out=new ObjectOutputStream(socket.getOutputStream());out.flush();
            in=new ObjectInputStream(socket.getInputStream());in.setObjectInputFilter(WireFilter.create());
            Thread reader=new Thread(()->{try{while(!socket.isClosed()){
                Message message=(Message)in.readObject();
                if(media!=null)switch(message.getType()){
                    case MEDIA_ICE_SERVERS -> {IceConfig value=(IceConfig)message.getPayload();if(value.turnAvailable()&&didReceiveTurn.compareAndSet(false,true))turnCredentials.countDown();media.onIceConfig(value);continue;}
                    case MEDIA_STATUS -> {media.onPeerState((PeerState)message.getPayload());continue;}
                    case MEDIA_DESCRIPTION -> {media.onDescription((Signal)message.getPayload());continue;}
                    case MEDIA_CANDIDATE -> {media.onCandidate((Signal)message.getPayload());continue;}
                    default -> {}
                }
                inbox.add(message);
            }}catch(Exception ignored){}},"media-real-server-reader");
            reader.setDaemon(true);reader.start();
        }
        synchronized void send(Message message){
            try{out.writeObject(message);out.reset();out.flush();}
            catch(IOException e){failure.compareAndSet(null,"Signaling connection failed");}
        }
        void send(MessageType type,Object payload){send(Message.of(type,payload));}
        Message take(MessageType type)throws Exception {
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
            while(System.nanoTime()<until){Message m=inbox.poll(100,TimeUnit.MILLISECONDS);if(m!=null&&m.getType()==type)return m;}
            throw new AssertionError("Timed out waiting for "+type);
        }
        void login(String name)throws Exception {
            send(MessageType.REGISTER_REQUEST,new LoginRequest(name,"secret123"));
            if(!((AuthResponse)take(MessageType.REGISTER_RESULT).getPayload()).success())throw new AssertionError("Registration failed");
            send(MessageType.LOGIN_REQUEST,new LoginRequest(name,"secret123"));
            if(!((AuthResponse)take(MessageType.LOGIN_RESULT).getPayload()).success())throw new AssertionError("Login failed");
            send(MessageType.MEDIA_SUPPORT,null);
        }
        void startMedia(String matchId){
            media=new MediaCallSession(this::send,status->{if(status.startsWith("Đã kết nối")&&didConnect.compareAndSet(false,true))connected.countDown();},
                text->{},text->failure.compareAndSet(null,text),on->{if(on)captured.set(true);},on->{if(on)captured.set(true);},image->{},image->{},devices->{});
            media.join(matchId);
        }
        public void close()throws Exception {if(media!=null)media.close(false);socket.close();}
    }
    public static void main(String[] args)throws Exception {
        System.setProperty("darts.db.url","jdbc:h2:mem:media_e2e;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        System.setProperty("darts.db.user","sa");System.setProperty("darts.db.password","");
        DBConnection.getInstance().initialize();
        try(ServerSocket server=new ServerSocket(0,50,InetAddress.getLoopbackAddress())){
            Thread accept=new Thread(()->{try{while(!server.isClosed())new Client(server.accept()).start();}catch(IOException ignored){}},"media-real-server-accept");
            accept.setDaemon(true);accept.start();
            try(Peer a=new Peer(server.getLocalPort());Peer b=new Peer(server.getLocalPort())){
                a.login("MediaAlice");b.login("MediaBob");
                a.send(MessageType.INVITE_REQUEST,"MediaBob");
                InviteResponse invite=(InviteResponse)b.take(MessageType.INVITE_RECEIVED).getPayload();
                b.send(MessageType.INVITE_ACCEPT,invite.id());
                MatchSnapshot first=(MatchSnapshot)a.take(MessageType.MATCH_STARTED).getPayload();
                MatchSnapshot second=(MatchSnapshot)b.take(MessageType.MATCH_STARTED).getPayload();
                if(!first.matchId().equals(second.matchId()))throw new AssertionError("Players have different matches");
                a.startMedia(first.matchId());b.startMedia(first.matchId());
                if(!connected.await(25,TimeUnit.SECONDS)||failure.get()!=null)throw new AssertionError("Media did not connect: "+failure.get());
                if(captured.get())throw new AssertionError("Media opened a device without user action");
                if(Boolean.parseBoolean(System.getenv("DARTS_REQUIRE_TURN"))&&!turnCredentials.await(10,TimeUnit.SECONDS))
                    throw new AssertionError("TURN credentials were not delivered to both participants");
                System.out.println("PASS two real game-server clients establish PvP media with devices off");
                if(Boolean.parseBoolean(System.getenv("DARTS_REQUIRE_TURN")))System.out.println("PASS real game server delivers TURN to both clients");
                a.send(MessageType.LEAVE_MATCH,first.matchId());a.take(MessageType.MATCH_FINISHED);b.take(MessageType.MATCH_FINISHED);
                System.out.println("PASS PvP match exits cleanly");
            }
        }
    }
}
