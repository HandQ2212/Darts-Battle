package services;

import enums.MatchMode;
import models.Room;
import protocol.Message;
import protocol.MessageType;
import protocol.dto.SessionMessages.*;

/** Serializes readiness and leaving on the same monitor as the current match. */
public final class SessionService {
    private SessionService(){}
    public static void broadcast(Room room,String notice){
        State state=new State(room.getSessionId(),room.getMatchId(),room.getSessionPhase(),room.getRoundIndex(),
            room.isRematchSupported(),room.isPlayer1Ready(),room.isPlayer2Ready(),
            room.getMode()==MatchMode.PVE?room.getBotDifficulty():null,notice);
        send(room.getPlayer1Id(),state);if(room.getPlayer2Id()!=null)send(room.getPlayer2Id(),state);
    }
    private static void send(long userId,State state){
        Client client=ClientManager.getInstance().getClient(userId);
        if(client!=null&&client.supportsSessions())client.send(Message.of(MessageType.SESSION_STATUS,state));
    }
    private static Room session(Client client,String id){
        Room room=RoomManager.getInstance().getSession(id);
        if(room==null||!room.containsUser(client.getLoggedInUserId()))throw new IllegalArgumentException("Phiên chơi không hợp lệ.");
        return room;
    }
    private static boolean remember(Room room,long userId,String id){
        if(id==null||id.isBlank()||id.length()>80)throw new IllegalArgumentException("Thiếu mã yêu cầu hợp lệ.");
        String key=userId+":"+id;if(room.sessionRequests.containsKey(key))return false;
        if(room.sessionRequests.size()>=128)room.sessionRequests.remove(room.sessionRequests.keySet().iterator().next());
        room.sessionRequests.put(key,true);return true;
    }
    public static void ready(Client client,ReadyRequest request){
        if(request==null)throw new IllegalArgumentException("Yêu cầu chơi tiếp không hợp lệ.");
        Room room=session(client,request.sessionId());long userId=client.getLoggedInUserId();
        synchronized(room){
            if(!remember(room,userId,request.requestId()))return;
            if(!room.isRematchSupported()||!"RESULTS".equals(room.getSessionPhase())||!room.getMatchId().equals(request.matchId()))
                throw new IllegalArgumentException("Ván chơi tiếp đã thay đổi hoặc không còn khả dụng.");
            if(room.startingNext){broadcast(room,"Đang chuẩn bị ván mới.");return;}
            room.setReady(userId,request.ready());
            if(room.isPlayer1Ready()&&(room.getMode()==MatchMode.PVE||room.isPlayer2Ready()))
                GameService.getInstance().startNextMatch(room);
            else broadcast(room,"Đang chờ người chơi sẵn sàng.");
        }
    }
    public static void leave(Client client,LeaveRequest request){
        if(request==null)throw new IllegalArgumentException("Yêu cầu rời phiên không hợp lệ.");
        Room room=session(client,request.sessionId());long userId=client.getLoggedInUserId();
        synchronized(room){if(!remember(room,userId,request.requestId()))return;GameService.getInstance().leaveSession(room,userId,"FORFEIT");}
    }
}
