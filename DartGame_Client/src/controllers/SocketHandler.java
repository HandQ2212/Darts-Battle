package controllers;
import btl_ltm_n3.Main;
import models.User;
import enums.*;
import protocol.*;
import protocol.dto.*;
import protocol.dto.GameMessages.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;

public class SocketHandler {
    public String loginUser;
    public long loggedInUserId=-1;
    public String competitor="";
    public String roomIdPresent;
    public boolean checkYouAreInvited;
    private volatile Connection current;
    public String lobbyNotice="";
    private ui.Notifications.Notice inviteDialog;
    private void closeInvite() {
        if(inviteDialog!=null)inviteDialog.close();
        inviteDialog=null;
    }
    private final ExecutorService connector=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"connect");t.setDaemon(true);return t;});
    private final ScheduledExecutorService heartbeat=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"heartbeat");t.setDaemon(true);return t;});
    private final java.util.concurrent.atomic.AtomicBoolean connecting = new java.util.concurrent.atomic.AtomicBoolean();
    private final ConcurrentLinkedQueue<Runnable> pendingConnect = new ConcurrentLinkedQueue<>();
    private volatile boolean stopped, connectionWanted;
    private volatile String connectionText = "Đang kết nối máy chủ...";
    private java.util.function.BiConsumer<Boolean,String> connectionListener;
    private boolean authPending;
    private static class Connection {
        Socket socket; Socket transport; ObjectInputStream in; ObjectOutputStream out;
        volatile boolean closed;
        final BlockingQueue<Message> queue=new ArrayBlockingQueue<>(128);
        Thread writer;
    }
    public SocketHandler() {
        heartbeat.scheduleAtFixedRate(()->{
            Connection c=current;
            if(c!=null&&!c.closed) sendData(Message.of(MessageType.PING,null));
            else if(connectionWanted&&!stopped) ensureConnected(null);
        },5,5,TimeUnit.SECONDS);
    }
    public boolean isConnected() { Connection c=current; return c!=null&&!c.closed; }
    public void setConnectionListener(java.util.function.BiConsumer<Boolean,String> listener) {
        connectionListener=listener;
        if(listener!=null) listener.accept(isConnected()&&!authPending,connectionText);
    }
    private void notifyConnection(String text) {
        connectionText=text;
        Runnable update=()->{if(connectionListener!=null)connectionListener.accept(isConnected()&&!authPending,text);};
        if(Platform.isFxApplicationThread())update.run();else Platform.runLater(update);
    }
    public void ensureConnected(Runnable afterConnected) {
        if(stopped)return;
        connectionWanted=true;
        if(afterConnected!=null)pendingConnect.add(afterConnected);
        Connection old=current;
        if(old!=null&&!old.closed){finishConnect(old);return;}
        if(!ConnectionSettings.configured()) {
            pendingConnect.clear();notifyConnection("Bộ cài chưa có địa chỉ máy chủ. Vui lòng liên hệ người tổ chức.");return;
        }
        if(!connecting.compareAndSet(false,true))return;
        notifyConnection("Đang kết nối máy chủ...");
        connector.execute(()->{
            Connection c=new Connection();
            try {
                if("websocket".equals(ConnectionSettings.transport())) {
                    Socket tunnel=network.WebSocketSocket.open(ConnectionSettings.websocketUri(),5000);c.transport=tunnel;
                    c.socket=network.TransportSecurity.secure(tunnel,ConnectionSettings.host(),ConnectionSettings.port(),5000,ConnectionSettings.tlsContext());
                }else c.socket=network.TransportSecurity.connect(ConnectionSettings.host(),ConnectionSettings.port(),5000,ConnectionSettings.tlsContext());
                c.socket.setSoTimeout(10000);c.socket.setTcpNoDelay(true);
                c.out=new ObjectOutputStream(c.socket.getOutputStream());c.out.flush();
                c.in=new ObjectInputStream(c.socket.getInputStream());c.in.setObjectInputFilter(WireFilter.create());
                c.socket.setSoTimeout(35000);
                synchronized(this){if(stopped){close(c);return;}current=c;}
                c.writer=new Thread(()->write(c),"server-writer");c.writer.setDaemon(true);c.writer.start();
                Thread reader=new Thread(()->listen(c),"server-reader");reader.setDaemon(true);reader.start();
                notifyConnection("Đã kết nối. Mời bạn đăng nhập.");finishConnect(c);
            } catch(Exception e) {
                close(c);pendingConnect.clear();
                String text=e instanceof javax.net.ssl.SSLException || e instanceof java.security.GeneralSecurityException
                        ? "Không xác minh được máy chủ. Vui lòng liên hệ người tổ chức."
                        : "Chưa kết nối được máy chủ. Đang tự thử lại...";
                System.err.println("Connection failed: "+e.getClass().getSimpleName());
                notifyConnection(text);
            } finally {connecting.set(false);}
        });
    }
    private void finishConnect(Connection c) {
        Platform.runLater(()->{if(current==c&&!c.closed){Runnable task;while((task=pendingConnect.poll())!=null)task.run();}});
    }
    private void write(Connection c) {
        try {while(!c.closed){Message m=c.queue.take();c.out.writeObject(m);c.out.reset();c.out.flush();}}
        catch(Exception e){failed(c);}
    }
    private void listen(Connection c) {
        try {
            while(!c.closed){
                Message m=(Message)c.in.readObject();
                if(m.getType()==MessageType.PONG)continue;
                Platform.runLater(()->{if(current==c&&!c.closed){try{receive(m);}catch(RuntimeException e){showError("Không xử lý được phản hồi server: "+e.getMessage());}}});
            }
        }catch(Exception e){failed(c);}
    }
    private synchronized void failed(Connection c) {
        if(current!=c||c.closed)return;
        current=null;close(c);
        Platform.runLater(()->{
            boolean playing=roomIdPresent!=null;
            clearSession();
            try{Main.setRoot("login");}catch(IOException e){e.printStackTrace();}
            notifyConnection("Đã mất kết nối. Đang tự kết nối lại...");
            if(playing)showError("Đã mất kết nối. Trận hiện tại kết thúc; hãy đăng nhập lại khi kết nối phục hồi.");
            ensureConnected(null);
        });
    }
    private void close(Connection c) {
        c.closed=true;if(c.writer!=null)c.writer.interrupt();
        try{if(c.transport!=null)c.transport.close();}catch(IOException ignored){}
        try{if(c.socket!=null)c.socket.close();}catch(IOException ignored){}
    }
    public synchronized void disconnect(){Connection c=current;current=null;if(c!=null)close(c);}
    public void shutdown(){stopped=true;connectionWanted=false;pendingConnect.clear();disconnect();connector.shutdownNow();heartbeat.shutdownNow();}
    public void sendData(Message m) {
        Connection c=current;
        if(c==null||c.closed){Platform.runLater(()->showError("Máy chủ chưa sẵn sàng. Vui lòng chờ kết nối phục hồi."));return;}
        if(!c.queue.offer(m))failed(c);
    }
    @SuppressWarnings("unchecked") private void receive(Message m) {
        switch(m.getType()) {
            case LOGIN_RESULT -> {
                authPending=false;notifyConnection("Đã kết nối. Mời bạn đăng nhập.");
                AuthResponse a=(AuthResponse)m.getPayload();
                if(!a.success()) {showError(a.message());break;}
                loginUser=a.username();loggedInUserId=a.userId();ConnectionSettings.rememberUsername(a.username());
                try {Main.setRoot("home");} catch(IOException e) {showError(e.getMessage());}
                getListOnline();
            }
            case REGISTER_RESULT -> {
                authPending=false;notifyConnection("Đã kết nối. Mời bạn đăng nhập.");
                AuthResponse a=(AuthResponse)m.getPayload();
                if(a.success()) {try {Main.setRoot("login");} catch(IOException e) {showError(e.getMessage());}
                    Main.notifications().toast("Sẵn sàng vào sân",a.message(),"success");}
                else showError(a.message());
            }
            case LOGOUT_RESULT -> {clearSession();try {Main.setRoot("login");} catch(IOException e) {showError(e.getMessage());}}
            case ONLINE_LIST -> {
                Main.listOnlineUser.clear();
                for(OnlineUser u:(List<OnlineUser>)m.getPayload()) if(u.id()!=loggedInUserId) {
                    User view=new User(u.id(),u.username(),"online");view.setStatus(u.status());Main.listOnlineUser.add(view);
                }
                if(Main.chooseOpponentController!=null)Main.chooseOpponentController.updateUserTable(Main.listOnlineUser);
            }
            case INVITE_RECEIVED -> {
                closeInvite();
                InviteResponse invitation=(InviteResponse)m.getPayload();
                inviteDialog=Main.notifications().invite(invitation.senderName(),invitation.expiresAt(),accepted->
                    sendData(Message.of(accepted?MessageType.INVITE_ACCEPT:MessageType.INVITE_REJECT,invitation.id())));
            }
            case INVITE_RESULT -> {
                closeInvite();
                lobbyNotice=(String)m.getPayload();
                if(Main.chooseOpponentController!=null)Main.chooseOpponentController.showStatus(lobbyNotice);
                Main.notifications().toast("Lời mời đấu",lobbyNotice,"info");
                getListOnline();
            }
            case MATCH_STARTED -> {
                closeInvite();
                MatchSnapshot s=(MatchSnapshot)m.getPayload();
                if(Objects.equals(roomIdPresent,s.matchId()))break;
                roomIdPresent=s.matchId();Main.gameViewState.begin(s,loggedInUserId);competitor=Main.gameViewState.getOpponentName();
                try {Main.setRoot("startgame");Main.startGameController.render(s);}catch(IOException e){showError(e.getMessage());}
            }
            case TURN_PREPARING, ROTATION_APPLIED, TURN_STARTED -> {
                MatchSnapshot s=(MatchSnapshot)m.getPayload();
                if(Main.gameViewState.apply(s)&&Main.startGameController!=null) {
                    if(m.getType()==MessageType.ROTATION_APPLIED)Main.startGameController.rotationConfirmed(s);
                    else Main.startGameController.render(s);
                }
            }
            case THROW_RESOLVED -> {
                ThrowResolvedResponse res=(ThrowResolvedResponse)m.getPayload();
                if(Main.gameViewState.apply(res.getSnapshot())&&Main.startGameController!=null)Main.startGameController.showThrow(res);
            }
            case MATCH_FINISHED -> {
                MatchSnapshot s=(MatchSnapshot)m.getPayload();
                if(!Objects.equals(roomIdPresent,s.matchId()))break;
                Main.gameViewState.apply(s); roomIdPresent=null;
                if(Main.startGameController!=null)Main.startGameController.finish(s);
            }
            case CHAT_MESSAGE -> {
                ChatMessage chat=(ChatMessage)m.getPayload();
                if(Objects.equals(roomIdPresent,chat.matchId())&&Main.startGameController!=null)Main.startGameController.addMessage(chat.senderName()+": "+chat.text());
            }
            case RANKING_RESULT -> {
                Main.leaderboardData=new ArrayList<>((List<String>)m.getPayload());
                if(Main.rankingController!=null)Main.rankingController.updateLeaderboardTable();
            }
            case HISTORY_RESULT -> {
                List<String> rows=new ArrayList<>();
                for(String row:(List<String>)m.getPayload()) {String[] p=row.split("\\|",-1);if(p.length>=6)rows.add(loginUser+"|"+p[2]+"|"+p[3]+"|"+p[5]);}
                if(Main.matchHistoryController!=null)Main.matchHistoryController.updateMatchHistory(rows);
            }
            case ERROR -> showError((String)m.getPayload());
            default -> {}
        }
    }
    private void clearSession() {authPending=false;closeInvite();lobbyNotice="";loginUser=null;loggedInUserId=-1;roomIdPresent=null;competitor="";Main.listOnlineUser.clear();Main.gameViewState.reset();}
    public void showError(String message) {
        authPending=false;notifyConnection(isConnected()?"Đã kết nối. Mời bạn đăng nhập.":connectionText);
        if(Main.startGameController!=null)Main.startGameController.commandFailed();
        Main.notifications().error(message);
    }
    public void login(String username,String password) {
        if(authPending)return;authPending=true;notifyConnection("Đang đăng nhập...");
        sendData(Message.of(MessageType.LOGIN_REQUEST,new LoginRequest(username,password)));
    }
    public void register(String username,String password) {
        if(authPending || !isConnected())return;authPending=true;notifyConnection("Đang đăng ký...");
        sendData(Message.of(MessageType.REGISTER_REQUEST,new LoginRequest(username,password)));
    }
    public void logout() {sendData(Message.of(MessageType.LOGOUT_REQUEST,null));}
    public void getListOnline() {sendData(Message.of(MessageType.ONLINE_LIST,null));}
    public void inviteToPlay(String opponent) {sendData(Message.of(MessageType.INVITE_REQUEST,opponent));}
    public void leaveGame() {sendData(Message.of(MessageType.LEAVE_MATCH,roomIdPresent));}
    public void createBotMatch() {sendData(Message.of(MessageType.CREATE_BOT_MATCH,null));}
    public void getLeaderboard(int limit) {sendData(Message.of(MessageType.GET_RANKING,limit));}
    public void getMatchHistory(String username) {sendData(Message.of(MessageType.GET_HISTORY,null));}
    public void getUserRank(String username) {}
    public void getUserStats(String username) {}
    public void submitThrowCoordinate(double x,double y) {
        sendData(Message.of(MessageType.THROW_COORDINATE,new ThrowCoordinateRequest(roomIdPresent,Main.gameViewState.getTurnId(),
                Main.gameViewState.getDartsThrown()+1,UUID.randomUUID().toString(),x,y)));
    }
}
