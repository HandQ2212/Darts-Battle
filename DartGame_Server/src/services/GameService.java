package services;
import controllers.MatchHistoryController;
import enums.*;
import models.*;
import protocol.*;
import protocol.dto.*;
import protocol.dto.GameMessages.*;
import java.util.*;

/** Every transition is atomic under its room monitor. Network writes only enqueue. */
public class GameService {
    private static final GameService INSTANCE=new GameService();
    private final DartboardScorer scorer=new DartboardScorer();
    private final MatchHistoryController history=new MatchHistoryController();
    private record AppliedRotation(long actor, RotationRequest request, MatchSnapshot snapshot) {}
    private GameService() {}
    public static GameService getInstance() { return INSTANCE; }
    public void startMatch(Room r) {
        synchronized(r) {
            if(r.started) throw new IllegalArgumentException("Trận đã bắt đầu.");
            // Fail before announcing a match when its initial record cannot be created.
            history.createMatch(r.getMatchId(),r.getMode().name(),r.getPlayer1Id(),r.getPlayer2Id(),r.getPlayer2Type().name());
            r.started=true;
            GameState s=r.getGameState(); s.setCurrentPlayerId(r.getPlayer1Id()); s.setCurrentActorType(ActorType.HUMAN);
            s.setStatus(MatchStatus.WAITING_ROTATION); r.version++; resetDeadline(r);
            ClientManager.getInstance().setStatus(r.getPlayer1Id(),PlayerStatus.IN_MATCH);
            if(r.getPlayer2Id()!=null) ClientManager.getInstance().setStatus(r.getPlayer2Id(),PlayerStatus.IN_MATCH);
            broadcast(r,Message.of(MessageType.MATCH_STARTED,snapshot(r)));
            broadcast(r,Message.of(MessageType.TURN_PREPARING,snapshot(r)));
        }
    }
    public void submitRotation(RotationRequest req,long userId) {
        if(req==null) throw invalid("Thiếu góc xoay.");
        Room r=room(req.matchId());
        synchronized(r) {
            member(r,userId); requestId(req.requestId());
            Object prior=r.requests.get(req.requestId());
            if(prior instanceof AppliedRotation old && old.actor()==userId && old.request().equals(req)) {
                ClientManager.getInstance().sendToUser(userId,Message.of(MessageType.ROTATION_APPLIED,old.snapshot())); return;
            }
            if(prior!=null) throw invalid("RequestId đã được sử dụng.");
            actor(r,userId,req.turnId(),MatchStatus.WAITING_ROTATION);
            if(req.degrees()<0 || req.degrees()>359) throw invalid("Góc xoay phải là số nguyên từ 0 đến 359.");
            applyRotation(r,req.degrees());
            remember(r,req.requestId(),new AppliedRotation(userId,req,snapshot(r)));
        }
    }
    private void applyRotation(Room r,int degree) {
        GameState s=r.getGameState(); s.setRotationDegree(degree); s.setStatus(MatchStatus.PLAYING); r.version++;
        broadcast(r,Message.of(MessageType.ROTATION_APPLIED,snapshot(r)));
        broadcast(r,Message.of(MessageType.TURN_STARTED,snapshot(r)));
    }
    public ThrowResolvedResponse submitThrow(ThrowCoordinateRequest req,Long userId) {
        if(req==null || userId==null) throw invalid("Lệnh ném không hợp lệ.");
        Room r=room(req.getMatchId());
        synchronized(r) {
            member(r,userId); requestId(req.getRequestId());
            Object prior=r.requests.get(req.getRequestId());
            if(prior instanceof ThrowResolvedResponse old && Objects.equals(old.getActorId(),userId)
                    && old.getTurnId()==req.getTurnId() && old.getDartIndex()==req.getDartIndex()
                    && old.getX()==req.getX() && old.getY()==req.getY()) {
                ClientManager.getInstance().sendToUser(userId,Message.of(MessageType.THROW_RESOLVED,old)); return old;
            }
            if(prior!=null) throw invalid("RequestId đã được sử dụng.");
            actor(r,userId,req.getTurnId(),MatchStatus.PLAYING);
            return resolveThrow(r,req,userId);
        }
    }
    private ThrowResolvedResponse resolveThrow(Room r,ThrowCoordinateRequest req,Long userId) {
        GameState s=r.getGameState();
        if(req.getDartIndex()!=s.getDartsThrown()+1 || s.getDartsThrown()>=3) throw invalid("Sai thứ tự phi tiêu.");
        if(!Double.isFinite(req.getX()) || !Double.isFinite(req.getY()) || Math.abs(req.getX())>10000 || Math.abs(req.getY())>10000)
            throw invalid("Tọa độ không hợp lệ.");
        boolean p1=Objects.equals(userId,r.getPlayer1Id());
        int remaining=p1?s.getPlayer1RemainingScore():s.getPlayer2RemainingScore();
        int score=scorer.calculateDartScore(req.getX(),req.getY(),s.getRotationDegree());
        boolean bust=score>remaining;
        if(!bust) remaining-=score;
        if(p1) s.setPlayer1RemainingScore(remaining); else s.setPlayer2RemainingScore(remaining);
        s.setDartsThrown(s.getDartsThrown()+1); r.version++;
        ThrowResolvedResponse res=new ThrowResolvedResponse();
        res.setMatchId(r.getMatchId()); res.setTurnId(s.getTurnId()); res.setActorId(userId);
        res.setRequestId(req.getRequestId()); res.setActorName(p1?r.player1Name:r.player2Name);
        res.setActorType(s.getCurrentActorType()); res.setDartIndex(s.getDartsThrown());
        res.setX(req.getX()); res.setY(req.getY()); res.setRotationDegree(s.getRotationDegree());
        res.setHitArea(scorer.calculateHitArea(req.getX(),req.getY(),s.getRotationDegree()));
        res.setDartScore(score); res.setRemainingScoreAfter(remaining); res.setBust(bust);
        res.setMatchFinished(remaining==0); res.setTurnFinished(remaining==0 || s.getDartsThrown()==3);
        if(res.isMatchFinished()) markFinished(r,userId,s.getCurrentActorType(),"ZERO_SCORE");
        res.setSnapshot(snapshot(r)); remember(r,req.getRequestId(),res);
        ThrowResult log=new ThrowResult();
        log.setMatchId(res.getMatchId()); log.setTurnId(res.getTurnId()); log.setActorType(res.getActorType());
        log.setDartIndex(res.getDartIndex()); log.setX(res.getX()); log.setY(res.getY());
        log.setRotationDegree(res.getRotationDegree()); log.setHitArea(res.getHitArea());
        log.setDartScore(res.getDartScore()); log.setRemainingScoreAfter(res.getRemainingScoreAfter());
        HistoryWriter.submit(r.getMatchId(),()->history.saveThrow(log,userId));
        broadcast(r,Message.of(MessageType.THROW_RESOLVED,res));
        if(res.isMatchFinished()) complete(r);
        else if(res.isTurnFinished()) switchTurn(r);
        return res;
    }
    private void switchTurn(Room r) {
        GameState s=r.getGameState(); boolean wasP1=Objects.equals(s.getCurrentPlayerId(),r.getPlayer1Id());
        s.setCurrentPlayerId(wasP1?r.getPlayer2Id():Long.valueOf(r.getPlayer1Id()));
        s.setCurrentActorType(wasP1?r.getPlayer2Type():ActorType.HUMAN);
        s.setTurnId(s.getTurnId()+1); s.setDartsThrown(0); s.setStatus(MatchStatus.WAITING_ROTATION);
        r.version++; resetDeadline(r); broadcast(r,Message.of(MessageType.TURN_PREPARING,snapshot(r)));
        if(s.getCurrentActorType()==ActorType.BOT) BotService.getInstance().playTurn(r.getMatchId(),s.getTurnId());
    }
    public void playBotStep(String matchId,long turnId,int dartIndex,int degree,double x,double y) {
        Room r=RoomManager.getInstance().getRoom(matchId); if(r==null) return;
        synchronized(r) {
            GameState s=r.getGameState();
            if(s.getTurnId()!=turnId || s.getCurrentActorType()!=ActorType.BOT || s.getStatus()==MatchStatus.FINISHED) return;
            if(dartIndex==0) { if(s.getStatus()==MatchStatus.WAITING_ROTATION) applyRotation(r,degree); return; }
            if(s.getStatus()!=MatchStatus.PLAYING || dartIndex!=s.getDartsThrown()+1) return;
            resolveThrow(r,new ThrowCoordinateRequest(matchId,turnId,dartIndex,"bot-"+turnId+"-"+dartIndex,x,y),null);
        }
    }
    public void leaveMatch(String matchId,long userId,String reason) {
        Room r=room(matchId);
        synchronized(r) {
            member(r,userId);
            if(r.getGameState().getStatus()==MatchStatus.FINISHED) return;
            Long opponent=r.getOpponentUserId(userId);
            markFinished(r,opponent,opponent==null?ActorType.BOT:ActorType.HUMAN,reason); r.version++; complete(r);
        }
    }
    public void expireTurns() {
        long now=System.currentTimeMillis();
        for(Room r:RoomManager.getInstance().allRooms()) synchronized(r) {
            if(!r.started || r.getGameState().getStatus()==MatchStatus.FINISHED || r.deadline>now) continue;
            Long actor=r.getGameState().getCurrentPlayerId();
            Long winner=actor==null?Long.valueOf(r.getPlayer1Id()):r.getOpponentUserId(actor);
            markFinished(r,winner,winner==null?ActorType.BOT:ActorType.HUMAN,"TURN_TIMEOUT"); r.version++; complete(r);
        }
    }
    private void markFinished(Room r,Long winner,ActorType type,String reason) {
        r.getGameState().setStatus(MatchStatus.FINISHED); r.winnerId=winner; r.winnerType=type; r.endReason=reason;
    }
    private void complete(Room r) {
        BotService.getInstance().cancel(r.getMatchId());
        HistoryWriter.submit(r.getMatchId(),()->history.finishMatch(r.getMatchId(),r.winnerId,r.winnerType.name(),r.endReason));
        broadcast(r,Message.of(MessageType.MATCH_FINISHED,snapshot(r)));
        RoomManager.getInstance().removeRoom(r.getMatchId());
        ClientManager.getInstance().setStatus(r.getPlayer1Id(),PlayerStatus.IDLE);
        if(r.getPlayer2Id()!=null) ClientManager.getInstance().setStatus(r.getPlayer2Id(),PlayerStatus.IDLE);
        ClientManager.getInstance().broadcastOnlineList();
    }
    private void resetDeadline(Room r) { r.deadline=System.currentTimeMillis()+Long.getLong("darts.turnTimeoutMillis",120000L); }
    public MatchSnapshot snapshot(Room r) {
        GameState s=r.getGameState();
        return new MatchSnapshot(r.getMatchId(),r.getMode(),r.getPlayer1Id(),r.player1Name,r.getPlayer2Id(),r.player2Name,
                s.getCurrentPlayerId(),s.getCurrentActorType(),s.getTurnId(),s.getDartsThrown(),s.getRotationDegree(),
                s.getPlayer1RemainingScore(),s.getPlayer2RemainingScore(),s.getStatus(),r.version,r.winnerId,r.winnerType,r.endReason,r.deadline);
    }
    private Room room(String id) { Room r=RoomManager.getInstance().getRoom(id); if(r==null) throw invalid("Trận không tồn tại hoặc đã kết thúc."); return r; }
    private void member(Room r,long id) { if(!r.started || !r.containsUser(id)) throw invalid("Bạn không thuộc trận này."); }
    private void actor(Room r,long id,long turn,MatchStatus phase) {
        GameState s=r.getGameState();
        if(s.getStatus()!=phase || s.getTurnId()!=turn || s.getCurrentActorType()!=ActorType.HUMAN || !Objects.equals(s.getCurrentPlayerId(),id))
            throw invalid("Sai lượt hoặc thao tác không hợp lệ ở giai đoạn hiện tại.");
    }
    private void requestId(String id) { if(id==null || id.isBlank() || id.length()>80) throw invalid("Thiếu requestId hợp lệ."); }
    private void remember(Room r,String key,Object value) {
        if(r.requests.size()>=32) r.requests.remove(r.requests.keySet().iterator().next()); r.requests.put(key,value);
    }
    private IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
    public void broadcast(Room r,Message m) {
        ClientManager.getInstance().sendToUser(r.getPlayer1Id(),m);
        if(r.getPlayer2Id()!=null) ClientManager.getInstance().sendToUser(r.getPlayer2Id(),m);
    }
}
