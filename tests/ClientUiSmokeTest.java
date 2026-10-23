import btl_ltm_n3.Main;
import controllers.*;
import enums.*;
import protocol.*;
import protocol.dto.*;
import protocol.dto.GameMessages.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Loads real FXML and drives aiming/state controls on the JavaFX application thread. */
public class ClientUiSmokeTest {
    static class FakeSocket extends SocketHandler {
        Message last;
        java.util.function.BiConsumer<Boolean,String> listener;
        @Override public void setConnectionListener(java.util.function.BiConsumer<Boolean,String> listener) {
            this.listener=listener;if(listener!=null)listener.accept(false,"Đang kết nối...");
        }
        @Override public void ensureConnected(Runnable ready) {}
        @Override public void sendData(Message m) {last=m;}
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS "+message);}
    static MatchSnapshot snap(long version,long turn,int darts,MatchStatus status,Long actor) {
        return new MatchSnapshot("ui-match",MatchMode.PVP,1,"Alice",2L,"Bobby",actor,ActorType.HUMAN,turn,darts,90,
                251,301,status,version,null,null,null,System.currentTimeMillis()+120000);
    }
    public static void main(String[] args) throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
        Platform.startup(()->{
            FakeSocket socket=new FakeSocket();Main.socketHandler=socket;socket.loginUser="Alice";socket.loggedInUserId=1;socket.competitor="Bobby";socket.roomIdPresent="ui-match";
            try {
                FXMLLoader loginLoader=new FXMLLoader(Main.class.getResource("/views/login.fxml"));Parent login=loginLoader.load();
                check(login.lookup("#serverHostField")==null&&login.lookup("#serverPortField")==null,"login asks for no IP or port");
                Button loginButton=(Button)login.lookup("#loginButton");check(loginButton.isDisabled(),"login waits for connection");
                check(socket.last==null,"opening app never submits credentials automatically");
                socket.listener.accept(true,"Đã kết nối");check(!loginButton.isDisabled(),"connected state enables login");
                ((TextField)login.lookup("#usernameField")).setText("Alice");((PasswordField)login.lookup("#passwordField")).setText("secret123");loginButton.fire();
                check(socket.last.getType()==MessageType.LOGIN_REQUEST,"manual login retained");
                for(String page:new String[]{"register","home","chooseOpponent","choosecomponent","ranking","match_history","notificationdialog"}) {
                    Parent p=FXMLLoader.load(Main.class.getResource("/views/"+page+".fxml"));check(p!=null,"FXML "+page);
                }
                MatchSnapshot initial=snap(1,1,0,MatchStatus.WAITING_ROTATION,1L);Main.gameViewState.begin(initial,1);
                FXMLLoader loader=new FXMLLoader(Main.class.getResource("/views/startgame.fxml"));Parent root=loader.load();StartGameController game=loader.getController();
                ((Button)root.lookup("#effectSoundButton")).fire();
                Button rotate=(Button)root.lookup("#rotateButton"),start=(Button)root.lookup("#startButton");TextField angle=(TextField)root.lookup("#angleInput");
                check(!rotate.isDisabled()&&start.isDisabled(),"rotation phase controls");
                angle.setText("90");game.handleRotateBoard();check(socket.last.getPayload() instanceof RotationRequest&&rotate.isDisabled(),"rotation waits for server");
                MatchSnapshot playing=snap(2,1,0,MatchStatus.PLAYING,1L);check(Main.gameViewState.apply(playing),"new version accepted");game.rotationConfirmed(playing);
                check(!start.isDisabled()&&angle.isDisabled()&&rotate.isDisabled(),"playing locks rotation");
                check(!Main.gameViewState.apply(playing)&&!Main.gameViewState.apply(initial),"duplicate/stale snapshots ignored");
                game.handleGameAction();game.handleGameAction();check(socket.last.getPayload() instanceof ThrowCoordinateRequest,"aim sends typed coordinate request");
                ThrowCoordinateRequest request=(ThrowCoordinateRequest)socket.last.getPayload();check(request.getDartIndex()==1&&request.getTurnId()==1&&request.getX()<0&&request.getY()<0,"relative coordinates and dart identity");
                check(start.isDisabled(),"one in-flight throw");
                ThrowResolvedResponse res=new ThrowResolvedResponse();res.setMatchId("ui-match");res.setTurnId(1);res.setActorId(1L);res.setDartIndex(1);res.setX(0);res.setY(0);res.setDartScore(50);res.setSnapshot(snap(3,1,1,MatchStatus.PLAYING,1L));
                Main.gameViewState.apply(res.getSnapshot());game.showThrow(res);check(!start.isDisabled(),"next dart enabled after result");
                MatchSnapshot other=snap(4,2,0,MatchStatus.WAITING_ROTATION,2L);Main.gameViewState.apply(other);game.render(other);
                check(start.isDisabled()&&rotate.isDisabled()&&angle.isDisabled(),"opponent turn locks all actions");
                game.dispose();socket.last=null;game.handleGameAction();check(socket.last==null,"disposed controller sends nothing");
                System.out.println("ALL UI SMOKE CHECKS PASSED");
            } catch(Throwable e){failure.set(e);} finally {socket.shutdown();done.countDown();}
        });
        if(!done.await(30,TimeUnit.SECONDS))throw new AssertionError("UI timeout");
        Platform.exit();if(failure.get()!=null)throw new AssertionError(failure.get());
    }
}
