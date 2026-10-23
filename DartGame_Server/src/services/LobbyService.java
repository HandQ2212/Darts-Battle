package services;
import enums.*;
import models.Room;
import protocol.*;
import protocol.dto.GameMessages.*;
import java.util.*;

/** Serializes session changes, invitations and reservations across both participants. */
public final class LobbyService {
    private static final LobbyService INSTANCE = new LobbyService();
    private final Map<String, Pending> invitations = new HashMap<>();
    private record Pending(String id, long sender, long receiver, long expires) {}
    private LobbyService() {}
    public static LobbyService getInstance() { return INSTANCE; }
    public synchronized void login(Client c, long id, String name) {
        if (c.isClosed()) return;
        c.setLoggedInUserId(id); c.setLoggedInUsername(name);
        if (!ClientManager.getInstance().addClient(c)) {
            c.setLoggedInUserId(-1); c.setLoggedInUsername(null);
            throw new IllegalArgumentException("Tài khoản đang đăng nhập ở máy khác.");
        }
        c.send(Message.of(MessageType.LOGIN_RESULT, new AuthResponse(true, id, name, "Đăng nhập thành công.")));
        ClientManager.getInstance().broadcastOnlineList();
    }
    public synchronized void logout(Client c, String reason) {
        long id = c.getLoggedInUserId();
        if (id == -1 || ClientManager.getInstance().getClient(id) != c) return;
        cancelInvitations(id, "Lời mời đã hủy vì người chơi rời đi.");
        // Remove the leaving socket first so it never receives a stale victory dialog.
        ClientManager.getInstance().removeClient(c);
        Room r = RoomManager.getInstance().findRoomOfUser(id);
        if (r != null) GameService.getInstance().leaveMatch(r.getMatchId(), id, reason);
        c.setLoggedInUserId(-1); c.setLoggedInUsername(null);
        ClientManager.getInstance().broadcastOnlineList();
    }
    public synchronized void handle(Client c, Message m) {
        expire();
        long id = c.getLoggedInUserId();
        if (id < 0 || ClientManager.getInstance().getClient(id) != c) throw new IllegalArgumentException("Hãy đăng nhập lại.");
        switch (m.getType()) {
            case ONLINE_LIST -> c.send(Message.of(MessageType.ONLINE_LIST, ClientManager.getInstance().onlineList()));
            case INVITE_REQUEST -> {
                Client target = ClientManager.getInstance().findByName((String)m.getPayload());
                if (target == null || target == c) throw new IllegalArgumentException("Đối thủ không online hoặc không hợp lệ.");
                long other = target.getLoggedInUserId();
                if (!idle(id) || !idle(other)) throw new IllegalArgumentException("Một trong hai người đang bận.");
                Pending p = new Pending(UUID.randomUUID().toString(), id, other, System.currentTimeMillis()+Long.getLong("darts.inviteTimeoutMillis",30000L));
                invitations.put(p.id(), p);
                ClientManager.getInstance().setStatus(id, PlayerStatus.INVITED);
                ClientManager.getInstance().setStatus(other, PlayerStatus.INVITED);
                target.send(Message.of(MessageType.INVITE_RECEIVED, new InviteResponse(p.id(), id, c.getLoggedInUsername(), p.expires())));
                c.send(Message.of(MessageType.INVITE_RESULT, "Đã gửi lời mời; chờ đối thủ trả lời (30 giây)."));
                ClientManager.getInstance().broadcastOnlineList();
            }
            case INVITE_ACCEPT, INVITE_REJECT -> {
                Pending p = invitations.get((String)m.getPayload());
                if (p == null || p.receiver()!=id) throw new IllegalArgumentException("Lời mời không hợp lệ hoặc đã hết hạn.");
                invitations.remove(p.id()); release(p);
                if (m.getType()==MessageType.INVITE_REJECT) { notifyBoth(p, "Lời mời đã bị từ chối."); break; }
                Client sender=ClientManager.getInstance().getClient(p.sender());
                if (sender==null || !idle(p.sender()) || !idle(id)) throw new IllegalArgumentException("Người chơi không còn sẵn sàng.");
                Room r=RoomManager.getInstance().createPvpRoom(p.sender(), id);
                start(r, sender.getLoggedInUsername(), c.getLoggedInUsername());
            }
            case CREATE_BOT_MATCH -> {
                if (!idle(id)) throw new IllegalArgumentException("Bạn đang bận; hãy kết thúc trận/lời mời hiện tại.");
                start(RoomManager.getInstance().createPveRoom(id), c.getLoggedInUsername(), "BOT");
            }
            case CHAT_MESSAGE -> {
                ChatMessage chat=(ChatMessage)m.getPayload();
                Room r=RoomManager.getInstance().getRoom(chat.matchId());
                if (r==null || !r.containsUser(id) || chat.text()==null || chat.text().isBlank() || chat.text().length()>500)
                    throw new IllegalArgumentException("Tin nhắn hoặc phòng không hợp lệ (tối đa 500 ký tự).");
                synchronized(r) {
                    if (r.getGameState().getStatus()==MatchStatus.FINISHED) throw new IllegalArgumentException("Trận đã kết thúc.");
                    GameService.getInstance().broadcast(r, Message.of(MessageType.CHAT_MESSAGE,
                            new ChatMessage(r.getMatchId(), chat.text().trim(), c.getLoggedInUsername())));
                }
            }
            default -> throw new IllegalArgumentException("Lệnh lobby không hợp lệ.");
        }
        ClientManager.getInstance().broadcastOnlineList();
    }
    private void start(Room r, String p1, String p2) {
        r.player1Name=p1; r.player2Name=p2;
        try { GameService.getInstance().startMatch(r); }
        catch (RuntimeException e) { RoomManager.getInstance().removeRoom(r.getMatchId()); throw e; }
    }
    private boolean idle(long id) {
        return ClientManager.getInstance().getStatus(id)==PlayerStatus.IDLE && RoomManager.getInstance().findRoomOfUser(id)==null;
    }
    private void release(Pending p) {
        ClientManager.getInstance().setStatus(p.sender(), PlayerStatus.IDLE);
        ClientManager.getInstance().setStatus(p.receiver(), PlayerStatus.IDLE);
    }
    private void notifyBoth(Pending p, String text) {
        Message m=Message.of(MessageType.INVITE_RESULT,text);
        ClientManager.getInstance().sendToUser(p.sender(),m); ClientManager.getInstance().sendToUser(p.receiver(),m);
    }
    private void cancelInvitations(long id, String text) {
        var it=invitations.values().iterator();
        while(it.hasNext()) { Pending p=it.next(); if(p.sender()==id || p.receiver()==id) {it.remove(); release(p); notifyBoth(p,text);} }
    }
    public synchronized void expire() {
        var it=invitations.values().iterator(); boolean changed=false;
        while(it.hasNext()) { Pending p=it.next(); if(p.expires()<System.currentTimeMillis()) {it.remove(); release(p); notifyBoth(p,"Lời mời đã hết hạn."); changed=true;} }
        if(changed) ClientManager.getInstance().broadcastOnlineList();
    }
}
