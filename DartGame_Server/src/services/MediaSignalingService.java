package services;

import enums.MatchMode;
import enums.MatchStatus;
import models.Room;
import protocol.Message;
import protocol.MessageType;
import protocol.dto.MediaMessages.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Authorizes and relays WebRTC setup messages. Audio/video remains peer-to-peer or TURN-relayed. */
public final class MediaSignalingService {
    private static final MediaSignalingService INSTANCE=new MediaSignalingService();
    private final ConcurrentHashMap<String,Session> sessions=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<JoinKey,JoinAttempt> pendingJoins=new ConcurrentHashMap<>();
    private final AtomicLong joinSequence=new AtomicLong();
    private final ExecutorService credentials=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"turn-credentials");t.setDaemon(true);return t;});
    private final ScheduledExecutorService refreshes=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"turn-refresh");t.setDaemon(true);return t;});
    private final TurnCredentialService turn=new TurnCredentialService();
    private MediaSignalingService(){refreshes.scheduleAtFixedRate(this::refreshCredentials,1,1,TimeUnit.MINUTES);}
    public static MediaSignalingService getInstance(){return INSTANCE;}

    public void join(Client client,String matchId) {
        long userId=client.getLoggedInUserId();
        if(!client.supportsMedia()){client.send(Message.of(MessageType.MEDIA_STATUS,new PeerState(matchId,"","",false,false,"Cập nhật client để dùng cuộc gọi.")));return;}
        Room room=RoomManager.getInstance().getSession(matchId);
        if(room==null||!room.started||room.getMode()!=MatchMode.PVP||!mediaAllowed(room)||!room.containsUser(userId))
            {client.send(Message.of(MessageType.MEDIA_STATUS,new PeerState(matchId,"","",false,false,"Chỉ người chơi trong trận 1v1 đang diễn ra mới tham gia cuộc gọi được.")));return;}
        Long opponentId=room.getOpponentUserId(userId);
        Client opponent=opponentId==null?null:ClientManager.getInstance().getClient(opponentId);
        if(opponent==null||!opponent.supportsMedia()) {
            client.send(Message.of(MessageType.MEDIA_STATUS,new PeerState(matchId,"",room.player2Name,false,false,
                    opponent==null?"Đối thủ chưa kết nối.":"Đối thủ cần cập nhật client để gọi.")));
            return;
        }
        String peerName=userId==room.getPlayer1Id()?room.player2Name:room.player1Name;
        JoinKey key=new JoinKey(matchId,userId);JoinAttempt attempt=new JoinAttempt(joinSequence.incrementAndGet());
        JoinAttempt previous=pendingJoins.put(key,attempt);
        if(previous!=null)synchronized(previous){previous.cancelled=true;}
        credentials.execute(()->finishJoin(client,room,userId,peerName,key,attempt));
    }

    private void finishJoin(Client client,Room room,long userId,String peerName,JoinKey key,JoinAttempt attempt) {
        var iceServers=turn.servers();
        if(client.isClosed()||ClientManager.getInstance().getClient(userId)!=client||RoomManager.getInstance().getSession(room.getSessionId())!=room||!mediaAllowed(room))return;
        Session session=sessions.compute(room.getSessionId(),(mapKey,old)->old==null?new Session(room):old);
        if(session.room!=room)return;
        Client p1=ClientManager.getInstance().getClient(room.getPlayer1Id());
        Client p2=ClientManager.getInstance().getClient(room.getPlayer2Id());
        if(p1==null||p2==null||!p1.supportsMedia()||!p2.supportsMedia()){
            client.send(Message.of(MessageType.MEDIA_STATUS,new PeerState(room.getSessionId(),session.id,peerName,false,false,"Đối thủ chưa hỗ trợ cuộc gọi.")));return;
        }
        synchronized(attempt) { synchronized(session) {
            if(attempt.cancelled||pendingJoins.get(key)!=attempt||sessions.get(room.getSessionId())!=session||RoomManager.getInstance().getSession(room.getSessionId())!=room){if(session.joined.isEmpty())sessions.remove(room.getSessionId(),session);return;}
            pendingJoins.remove(key,attempt);
            session.joined.add(userId);
            boolean both=session.joined.contains(room.getPlayer1Id())&&session.joined.contains(room.getPlayer2Id());
            boolean turnAvailable=iceServers.stream().anyMatch(s->s.urls().stream().anyMatch(u->u.startsWith("turn:")||u.startsWith("turns:")));
            if(turnAvailable)session.nextRefreshAt=Math.min(session.nextRefreshAt,System.currentTimeMillis()+TimeUnit.MINUTES.toMillis(45));
            else if(turn.configured())session.nextRefreshAt=Math.min(session.nextRefreshAt,System.currentTimeMillis()+TimeUnit.MINUTES.toMillis(5));
            client.send(Message.of(MessageType.MEDIA_ICE_SERVERS,new IceConfig(room.getSessionId(),session.id,iceServers,turnAvailable)));
            if(both) {
                p1.send(Message.of(MessageType.MEDIA_STATUS,new PeerState(room.getSessionId(),session.id,room.player2Name,true,true,"Cả hai đã vào cuộc gọi.")));
                p2.send(Message.of(MessageType.MEDIA_STATUS,new PeerState(room.getSessionId(),session.id,room.player1Name,true,false,"Cả hai đã vào cuộc gọi.")));
            } else client.send(Message.of(MessageType.MEDIA_STATUS,new PeerState(room.getSessionId(),session.id,peerName,false,userId==room.getPlayer1Id(),"Đang chờ đối thủ tham gia.")));
        }}
    }

    public void leave(Client client,String matchId) {
        if(matchId==null)return;
        long userId=client.getLoggedInUserId();
        Room room=RoomManager.getInstance().getSession(matchId);
        if(room==null||!room.containsUser(userId))return;
        JoinKey key=new JoinKey(matchId,userId);JoinAttempt attempt=pendingJoins.remove(key);
        if(attempt!=null)synchronized(attempt){attempt.cancelled=true;}
        Session session=sessions.get(matchId);if(session==null)return;
        synchronized(session) {
            if(!session.joined.contains(userId))return;
            if(sessions.remove(matchId,session)) {
                Client p1=ClientManager.getInstance().getClient(session.room.getPlayer1Id());
                Client p2=ClientManager.getInstance().getClient(session.room.getPlayer2Id());
                String name=client.getLoggedInUsername()==null?"Đối thủ":client.getLoggedInUsername();
                PeerState ended=new PeerState(matchId,session.id,name,false,false,"Cuộc gọi đã kết thúc. Cả hai có thể tham gia lại.");
                if(p1!=null)p1.send(Message.of(MessageType.MEDIA_STATUS,ended));
                if(p2!=null)p2.send(Message.of(MessageType.MEDIA_STATUS,ended));
            }
        }
    }

    public void disconnected(Client client) {
        long userId=client.getLoggedInUserId();
        if(userId<=0)return;
        pendingJoins.forEach((key,attempt)->{if(key.userId()==userId&&pendingJoins.remove(key,attempt))synchronized(attempt){attempt.cancelled=true;}});
        for(Session session:sessions.values())if(session.room.containsUser(userId))leave(client,session.room.getSessionId());
    }
    public void roomEnded(String matchId) {
        pendingJoins.forEach((key,attempt)->{if(key.matchId().equals(matchId)&&pendingJoins.remove(key,attempt))synchronized(attempt){attempt.cancelled=true;}});
        Session session=sessions.remove(matchId);if(session==null)return;
        PeerState ended=new PeerState(matchId,session.id,"",false,false,"Trận đã kết thúc.");
        if(session.room.getPlayer1Id()>0)ClientManager.getInstance().sendToUser(session.room.getPlayer1Id(),Message.of(MessageType.MEDIA_STATUS,ended));
        if(session.room.getPlayer2Id()!=null)ClientManager.getInstance().sendToUser(session.room.getPlayer2Id(),Message.of(MessageType.MEDIA_STATUS,ended));
    }

    private void refreshCredentials(){
        long now=System.currentTimeMillis();
        for(Session session:sessions.values()){
            java.util.List<Long> joined;
            synchronized(session){if(session.nextRefreshAt>now||!session.refreshing.compareAndSet(false,true))continue;joined=java.util.List.copyOf(session.joined);}
            if(joined.isEmpty()){synchronized(session){session.refreshing.set(false);}continue;}
            credentials.execute(()->{
                boolean renewedAll=true;
                try {
                    for(long userId:joined){
                        var servers=turn.servers();
                        boolean available=servers.stream().anyMatch(s->s.urls().stream().anyMatch(u->u.startsWith("turn:")||u.startsWith("turns:")));
                        if(!available){renewedAll=false;continue;}
                        Client client=ClientManager.getInstance().getClient(userId);
                        if(client==null||!client.supportsMedia()){renewedAll=false;continue;}
                        synchronized(session){if(sessions.get(session.room.getSessionId())==session&&session.joined.contains(userId))client.send(Message.of(MessageType.MEDIA_ICE_SERVERS,new IceConfig(session.room.getSessionId(),session.id,servers,true)));}
                    }
                } catch(RuntimeException failure) {
                    renewedAll=false;
                } finally {synchronized(session){session.nextRefreshAt=System.currentTimeMillis()+TimeUnit.MINUTES.toMillis(renewedAll?45:5);session.refreshing.set(false);}}
            });
        }
    }

    public void signal(Client client,Signal incoming,boolean candidate) {
        long userId=client.getLoggedInUserId();
        if(incoming==null||incoming.matchId()==null||incoming.callId()==null)throw invalid("Bản tin cuộc gọi không hợp lệ.");
        Session session=sessions.get(incoming.matchId());
        if(session==null||!session.id.equals(incoming.callId()))throw invalid("Cuộc gọi đã hết hạn.");
        int limit=candidate?2048:65536;
        if(incoming.value()==null||incoming.value().isBlank()||incoming.value().length()>limit)throw invalid("Dữ liệu thiết lập cuộc gọi vượt giới hạn.");
        if(candidate&&(incoming.mLineIndex()<0||incoming.mLineIndex()>31||incoming.mid()==null||incoming.mid().length()>128))throw invalid("ICE candidate không hợp lệ.");
        if(!candidate&&!("OFFER".equals(incoming.descriptionType())||"ANSWER".equals(incoming.descriptionType())))throw invalid("Loại SDP không hợp lệ.");
        Client peer;
        synchronized(session) {
            if(sessions.get(incoming.matchId())!=session||!session.joined.contains(userId)||
                    !session.joined.contains(session.room.getOpponentUserId(userId)))throw invalid("Hai người chơi chưa cùng tham gia cuộc gọi.");
            if(!candidate&&("OFFER".equals(incoming.descriptionType())!=(userId==session.room.getPlayer1Id())))throw invalid("Chỉ người mở cuộc gọi mới có thể gửi lời mời SDP.");
            long now=System.currentTimeMillis();Rate rate=session.rates.computeIfAbsent(userId,k->new Rate());
            if(now-rate.started>1000){rate.started=now;rate.count=0;}
            if(candidate&&++rate.candidates>256)throw invalid("Số lượng ICE candidate vượt giới hạn cuộc gọi.");
            if(++rate.count>(candidate?64:12))throw invalid("Gửi thiết lập cuộc gọi quá nhanh.");
            peer=ClientManager.getInstance().getClient(session.room.getOpponentUserId(userId));
        }
        if(peer==null||!peer.supportsMedia())throw invalid("Đối thủ đã ngắt kết nối.");
        Signal stamped=new Signal(incoming.matchId(),incoming.callId(),candidate?"ICE":incoming.descriptionType(),incoming.value(),incoming.mid(),incoming.mLineIndex(),userId);
        peer.send(Message.of(candidate?MessageType.MEDIA_CANDIDATE:MessageType.MEDIA_DESCRIPTION,stamped));
    }

    private static IllegalArgumentException invalid(String message){return new IllegalArgumentException(message);}
    private static boolean mediaAllowed(Room room){return room.getGameState().getStatus()!=MatchStatus.FINISHED||room.isRematchSupported()&&"RESULTS".equals(room.getSessionPhase());}
    private record JoinKey(String matchId,long userId){}
    private static final class JoinAttempt {final long id;boolean cancelled;JoinAttempt(long id){this.id=id;}}
    private static final class Session {
        final Room room;final String id=UUID.randomUUID().toString();final Set<Long> joined=new HashSet<>();final ConcurrentHashMap<Long,Rate> rates=new ConcurrentHashMap<>();
        final java.util.concurrent.atomic.AtomicBoolean refreshing=new java.util.concurrent.atomic.AtomicBoolean();volatile long nextRefreshAt=Long.MAX_VALUE;
        Session(Room room){this.room=room;}
    }
    private static final class Rate {long started=System.currentTimeMillis();int count,candidates;}
}
