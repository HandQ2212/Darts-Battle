package services;
import protocol.*;
import controllers.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client extends Thread {
    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private volatile long loggedInUserId=-1;
    private volatile String loggedInUsername;
    private volatile boolean mediaSupported;
    private final AtomicBoolean closed=new AtomicBoolean();
    private final BlockingQueue<Message> outgoing=new ArrayBlockingQueue<>(256);
    private final UserController users=new UserController();
    private final LobbyController lobby=new LobbyController();
    private final GameController game=new GameController();
    private final RankingController ranking=new RankingController();
    private Thread writer;
    private static final ExecutorService CLEANUP=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"session-cleanup");t.setDaemon(true);return t;});
    public Client(Socket socket) { this.socket=socket; setName("client-reader"); }
    @Override public void run() {
        try {
            socket.setSoTimeout(10000); socket.setTcpNoDelay(true);
            if (socket instanceof javax.net.ssl.SSLSocket tls) tls.startHandshake();
            out=new ObjectOutputStream(socket.getOutputStream()); out.flush();
            writer=new Thread(()->{
                try { while(!closed.get()) { Message m=outgoing.take(); out.writeObject(m); out.reset(); out.flush(); } }
                catch(Exception e) { close(); }
            },"client-writer"); writer.setDaemon(true); writer.start();
            in=new ObjectInputStream(socket.getInputStream()); in.setObjectInputFilter(WireFilter.create());
            socket.setSoTimeout(45000);
            while(!closed.get()) {
                Object value=in.readObject();
                if(!(value instanceof Message message) || message.getType()==null) { error("Bản tin không hợp lệ."); continue; }
                try { handleMessage(message); }
                catch(IllegalArgumentException | ClassCastException | NullPointerException e) { error(e.getMessage()==null?"Payload không hợp lệ.":e.getMessage()); }
                catch(RuntimeException e) { System.err.println("Request failed: "+e); error("Server không xử lý được yêu cầu. Hãy thử lại."); }
            }
        } catch(Exception e) { if(!closed.get()) System.out.println("Connection ended: "+e.getClass().getSimpleName()); }
        finally { close(); }
    }
    private void handleMessage(Message m) {
        if(m.getType()==MessageType.PING) { send(Message.of(MessageType.PONG,null)); return; }
        if(m.getType()==MessageType.LOGIN_REQUEST || m.getType()==MessageType.REGISTER_REQUEST || m.getType()==MessageType.LOGOUT_REQUEST) {
            users.handleMessage(this,m); return;
        }
        if(loggedInUserId<0 || ClientManager.getInstance().getClient(loggedInUserId)!=this) throw new IllegalArgumentException("Bạn cần đăng nhập.");
        switch(m.getType()) {
            case MEDIA_SUPPORT -> mediaSupported=true;
            case MEDIA_JOIN -> MediaSignalingService.getInstance().join(this,((protocol.dto.MediaMessages.JoinRequest)m.getPayload()).matchId());
            case MEDIA_LEAVE -> MediaSignalingService.getInstance().leave(this,(String)m.getPayload());
            case MEDIA_DESCRIPTION -> MediaSignalingService.getInstance().signal(this,(protocol.dto.MediaMessages.Signal)m.getPayload(),false);
            case MEDIA_CANDIDATE -> MediaSignalingService.getInstance().signal(this,(protocol.dto.MediaMessages.Signal)m.getPayload(),true);
            case ONLINE_LIST, INVITE_REQUEST, INVITE_ACCEPT, INVITE_REJECT, CREATE_BOT_MATCH, CHAT_MESSAGE -> lobby.handleMessage(this,m);
            case ROTATION_SUBMIT, THROW_COORDINATE, LEAVE_MATCH -> game.handleMessage(this,m);
            case GET_RANKING, GET_HISTORY -> ranking.handleMessage(this,m);
            default -> error("Lệnh không được hỗ trợ.");
        }
    }
    public void error(String message) { send(Message.of(MessageType.ERROR,message)); }
    public void send(Message m) { if(!closed.get() && !outgoing.offer(m)) close(); }
    public void close() {
        if(!closed.compareAndSet(false,true)) return;
        MediaSignalingService.getInstance().disconnected(this);
        try { if(socket!=null) socket.close(); } catch(IOException ignored) {}
        if(writer!=null) writer.interrupt();
        CLEANUP.execute(()->LobbyService.getInstance().logout(this,"DISCONNECT"));
    }
    public boolean isClosed() { return closed.get(); }
    public boolean supportsMedia(){return mediaSupported;}
    public long getLoggedInUserId() { return loggedInUserId; }
    public void setLoggedInUserId(long id) { loggedInUserId=id; }
    public String getLoggedInUsername() { return loggedInUsername; }
    public void setLoggedInUsername(String name) { loggedInUsername=name; }
}
