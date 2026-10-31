package controllers;
import btl_ltm_n3.Main;
import models.User;
import enums.*;
import protocol.*;
import protocol.dto.*;
import protocol.dto.GameMessages.*;
import protocol.dto.SessionMessages.*;
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
    public String sessionIdPresent;
    public State sessionState;
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
        final String id=UUID.randomUUID().toString();
        Socket socket; Socket transport; ObjectInputStream in; ObjectOutputStream out;
        volatile boolean closed;
        final BlockingQueue<Message> queue=new ArrayBlockingQueue<>(128);
        Thread writer;
    }
    public SocketHandler() {
        network.DiagnosticLog.configure("client");
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
            network.DiagnosticLog.event("connect_start","connection="+c.id+" transport="+ConnectionSettings.transport());
            try {
                if("websocket".equals(ConnectionSettings.transport())) {
                    Socket tunnel=network.WebSocketSocket.open(ConnectionSettings.websocketUri(),5000);c.transport=tunnel;
                    c.socket=network.TransportSecurity.secure(tunnel,ConnectionSettings.host(),ConnectionSettings.port(),5000,ConnectionSettings.tlsContext());
                }else c.socket=network.TransportSecurity.connect(ConnectionSettings.host(),ConnectionSettings.port(),5000,ConnectionSettings.tlsContext());
                c.socket.setSoTimeout(10000);c.socket.setTcpNoDelay(true);
                c.out=new ObjectOutputStream(new BufferedOutputStream(c.socket.getOutputStream(),16384));c.out.flush();
                c.in=new ObjectInputStream(c.socket.getInputStream());c.in.setObjectInputFilter(WireFilter.create());
                c.socket.setSoTimeout(35000);
                synchronized(this){if(stopped){close(c);return;}current=c;}
                network.DiagnosticLog.event("connect_ok","connection="+c.id);
                c.writer=new Thread(()->write(c),"server-writer");c.writer.setDaemon(true);c.writer.start();
                Thread reader=new Thread(()->listen(c),"server-reader");reader.setDaemon(true);reader.start();
                notifyConnection("Đã kết nối. Mời bạn đăng nhập.");finishConnect(c);
            } catch(Exception e) {
                network.DiagnosticLog.event("connect_error","connection="+c.id+" error="+network.DiagnosticLog.type(e));
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
        catch(Exception e){network.DiagnosticLog.event("write_error","connection="+c.id+" error="+network.DiagnosticLog.type(e));failed(c);}
    }
    private void listen(Connection c) {
        try {
            while(!c.closed){
                Message m=(Message)c.in.readObject();
                if(m.getType()==MessageType.PONG)continue;
                Platform.runLater(()->{if(current==c&&!c.closed){try{receive(m);}catch(RuntimeException e){showError("Không xử lý được phản hồi server: "+e.getMessage());}}});
            }
        }catch(Exception e){network.DiagnosticLog.event("read_error","connection="+c.id+" error="+network.DiagnosticLog.type(e));failed(c);}
    }
    private synchronized void failed(Connection c) {
        if(current!=c||c.closed)return;
        network.DiagnosticLog.event("connection_lost","connection="+c.id+" userId="+loggedInUserId+" inMatch="+(roomIdPresent!=null));
        current=null;close(c);
        Platform.runLater(()->{
            boolean playing=roomIdPresent!=null;
            if(Main.startGameController!=null)Main.startGameController.mediaDisconnected();
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
        if(!c.queue.offer(m)){network.DiagnosticLog.event("outgoing_queue_full","connection="+c.id);failed(c);}
    }
    @SuppressWarnings("unchecked") private void receive(Message m) {
        switch(m.getType()) {
            case LOGIN_RESULT -> {
                authPending=false;notifyConnection("Đã kết nối. Mời bạn đăng nhập.");
                AuthResponse a=(AuthResponse)m.getPayload();
                if(!a.success()) {showError(a.message());break;}
                loginUser=a.username();loggedInUserId=a.userId();ConnectionSettings.rememberUsername(a.username());
                network.DiagnosticLog.event("login_ok","connection="+(current==null?"none":current.id)+" userId="+loggedInUserId);
                try {Main.setRoot("home");} catch(IOException e) {showError(e.getMessage());}
                sendData(Message.of(MessageType.FEATURE_SUPPORT,null));
                sendData(Message.of(MessageType.MEDIA_SUPPORT,null));
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
                boolean next=sessionState!=null&&sessionState.rematchSupported()&&Objects.equals(sessionState.matchId(),s.matchId())
                    &&roomIdPresent!=null&&Main.startGameController!=null;
                roomIdPresent=s.matchId();Main.gameViewState.begin(s,loggedInUserId);competitor=Main.gameViewState.getOpponentName();
                if(next)Main.startGameController.nextMatch(s);
                else try {Main.setRoot("startgame");Main.startGameController.render(s);
                    if(sessionState!=null&&!sessionState.rematchSupported()&&sessionState.notice()!=null&&!sessionState.notice().isBlank())
                        Main.notifications().toast("Chơi tiếp",sessionState.notice(),"info");
                }catch(IOException e){showError(e.getMessage());}
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
                if(Main.gameViewState.applyThrow(res)&&Main.startGameController!=null)Main.startGameController.showThrow(res);
            }
            case MATCH_FINISHED -> {
                MatchSnapshot s=(MatchSnapshot)m.getPayload();
                if(!Objects.equals(roomIdPresent,s.matchId()))break;
                Main.gameViewState.apply(s);
                if(sessionState==null||!sessionState.rematchSupported())roomIdPresent=null;
                if(Main.startGameController!=null)Main.startGameController.finish(s);
            }
            case SESSION_STATUS -> {
                State state=(State)m.getPayload();
                if(state==null||(sessionIdPresent!=null&&!Objects.equals(sessionIdPresent,state.sessionId())&&roomIdPresent!=null))break;
                sessionIdPresent=state.sessionId();sessionState=state;
                if("CLOSED".equals(state.phase())){
                    if(state.rematchSupported()&&Main.startGameController!=null)Main.startGameController.sessionClosed(state.notice());
                    sessionIdPresent=null;sessionState=null;roomIdPresent=null;
                } else if(Main.startGameController!=null)Main.startGameController.sessionChanged(state);
            }
            case CHAT_MESSAGE -> {
                ChatMessage chat=(ChatMessage)m.getPayload();
                if((Objects.equals(roomIdPresent,chat.matchId())||Objects.equals(sessionIdPresent,chat.matchId()))&&Main.startGameController!=null)Main.startGameController.addMessage(chat.senderName()+": "+chat.text());
            }
            case MEDIA_STATUS -> {if(Main.startGameController!=null)Main.startGameController.mediaPeerState((protocol.dto.MediaMessages.PeerState)m.getPayload());}
            case MEDIA_ICE_SERVERS -> {if(Main.startGameController!=null)Main.startGameController.mediaIceServers((protocol.dto.MediaMessages.IceConfig)m.getPayload());}
            case MEDIA_DESCRIPTION -> {if(Main.startGameController!=null)Main.startGameController.mediaDescription((protocol.dto.MediaMessages.Signal)m.getPayload());}
            case MEDIA_CANDIDATE -> {if(Main.startGameController!=null)Main.startGameController.mediaCandidate((protocol.dto.MediaMessages.Signal)m.getPayload());}
            case RANKING_RESULT -> {
                Main.leaderboardData=new ArrayList<>((List<String>)m.getPayload());
                if(Main.rankingController!=null)Main.rankingController.updateLeaderboardTable();
            }
            case HISTORY_RESULT -> {
                List<String> rows=new ArrayList<>();
                for(String row:(List<String>)m.getPayload()) {String[] p=row.split("\\|",-1);if(p.length>=6)rows.add(loginUser+"|"+p[2]+"|"+p[3]+"|"+p[5]);}
                if(Main.matchHistoryController!=null)Main.matchHistoryController.updateMatchHistory(rows);
            }
            case RANKING_V2_RESULT -> {if(Main.rankingController!=null)Main.rankingController.updateRankings((protocol.dto.HistoryMessages.Rankings)m.getPayload());}
            case PLAYER_MATCHES_RESULT -> {if(Main.matchHistoryController!=null)Main.matchHistoryController.updatePage((protocol.dto.HistoryMessages.Page)m.getPayload());}
            case MATCH_DETAIL_RESULT -> {if(Main.matchDetailController!=null)Main.matchDetailController.updateDetail((protocol.dto.HistoryMessages.Detail)m.getPayload());}
            case ERROR -> showError((String)m.getPayload());
            default -> {}
        }
    }
    private void clearSession() {authPending=false;closeInvite();lobbyNotice="";loginUser=null;loggedInUserId=-1;roomIdPresent=null;sessionIdPresent=null;sessionState=null;competitor="";Main.listOnlineUser.clear();Main.gameViewState.reset();}
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
    public void logout() {network.DiagnosticLog.event("logout_requested","userId="+loggedInUserId);sendData(Message.of(MessageType.LOGOUT_REQUEST,null));}
    public void getListOnline() {sendData(Message.of(MessageType.ONLINE_LIST,null));}
    public void inviteToPlay(String opponent) {sendData(Message.of(MessageType.INVITE_REQUEST,opponent));}
    public void leaveGame() {sendData(Message.of(MessageType.LEAVE_MATCH,roomIdPresent));}
    public void createBotMatch() {sendData(Message.of(MessageType.CREATE_BOT_MATCH,null));}
    public void createBotMatch(BotDifficulty difficulty) {sendData(Message.of(MessageType.CREATE_BOT_MATCH,new BotMatchRequest(difficulty)));}
    public void setRematchReady(boolean ready) {
        if(sessionIdPresent!=null&&roomIdPresent!=null)sendData(Message.of(MessageType.SESSION_READY,new ReadyRequest(sessionIdPresent,roomIdPresent,UUID.randomUUID().toString(),ready)));
    }
    public void leaveSession() {if(sessionIdPresent!=null)sendData(Message.of(MessageType.SESSION_LEAVE,new LeaveRequest(sessionIdPresent,UUID.randomUUID().toString())));}
    public void getLeaderboard(int limit) {sendData(Message.of(MessageType.GET_RANKING,limit));}
    public void getMatchHistory(String username) {sendData(Message.of(MessageType.GET_HISTORY,null));}
    public void getRankingsV2(){sendData(Message.of(MessageType.RANKING_V2_REQUEST,null));}
    public void getHistoryPage(long playerId,int page){sendData(Message.of(MessageType.PLAYER_MATCHES_REQUEST,new protocol.dto.HistoryMessages.PageRequest(playerId,page,"ranking".equals(Main.historyOrigin))));}
    public void getMatchDetail(String matchId){sendData(Message.of(MessageType.MATCH_DETAIL_REQUEST,new protocol.dto.HistoryMessages.DetailRequest(matchId)));}
    public void getUserRank(String username) {}
    public void getUserStats(String username) {}
    public void submitThrowCoordinate(double x,double y) {
        sendData(Message.of(MessageType.THROW_COORDINATE,new ThrowCoordinateRequest(roomIdPresent,Main.gameViewState.getTurnId(),
                Main.gameViewState.getDartsThrown()+1,UUID.randomUUID().toString(),x,y)));
    }
}
