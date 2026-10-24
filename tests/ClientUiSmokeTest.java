import btl_ltm_n3.Main;
import controllers.*;
import enums.*;
import protocol.*;
import protocol.dto.*;
import protocol.dto.GameMessages.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
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
    static void preview(Parent root,String name,int width,int height) throws Exception {
        if(root.getScene()==null)new Scene(root,width,height);
        root.resize(width,height);root.applyCss();root.layout();
        String output=System.getenv("DARTS_UI_PREVIEW");
        if(output!=null) {
            javafx.scene.image.WritableImage image=root.snapshot(null,null);
            java.awt.image.BufferedImage png=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++)png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
            java.nio.file.Path file=java.nio.file.Path.of(output,name+".png");java.nio.file.Files.createDirectories(file.getParent());
            javax.imageio.ImageIO.write(png,"png",file.toFile());
        }
    }
    static void fits(Parent root,String id) {
        Node node=root.lookup(id);var bounds=node.localToScene(node.getBoundsInLocal());
        check(bounds.getMinX()>=0&&bounds.getMinY()>=0&&bounds.getMaxX()<=root.getLayoutBounds().getWidth()+1
                &&bounds.getMaxY()<=root.getLayoutBounds().getHeight()+1,id+" fits minimum game window");
    }
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
                preview(login,"login",1080,740);
                for(String page:new String[]{"register","home","chooseOpponent","choosecomponent","ranking","match_history","notificationdialog"}) {
                    Parent p=FXMLLoader.load(Main.class.getResource("/views/"+page+".fxml"));check(p!=null,"FXML "+page);
                    preview(p,page,page.equals("notificationdialog")?420:1100,page.equals("notificationdialog")?220:740);
                }
                MatchSnapshot initial=snap(1,1,0,MatchStatus.WAITING_ROTATION,1L);Main.gameViewState.begin(initial,1);
                FXMLLoader loader=new FXMLLoader(Main.class.getResource("/views/startgame.fxml"));Parent root=loader.load();StartGameController game=loader.getController();
                ((Button)root.lookup("#effectSoundButton")).fire();
                Button rotate=(Button)root.lookup("#rotateButton"),start=(Button)root.lookup("#startButton");TextField angle=(TextField)root.lookup("#angleInput");
                preview(root,"game-rotation",1160,800);
                javafx.scene.layout.Region surface=(javafx.scene.layout.Region)root.lookup("#gamePane");
                check(surface.getWidth()==450&&surface.getHeight()==400,"board coordinate surface remains 450 by 400");
                check(((Label)root.lookup("#turnLabel")).getText().contains("lượt bạn"),"current turn is visible");
                check(root.lookupAll(".score-value").size()==2,"both remaining scores visible");
                preview(root,"game-minimum",1040,780);
                for(String id:new String[]{"#gamePane","#scoreboardPane","#startButton","#rotateButton","#chatInput"})fits(root,id);
                for(String id:new String[]{"#rulesLabel","#bustRuleLabel"}) {
                    Label rule=(Label)root.lookup(id);
                    check(rule.getHeight()>=rule.prefHeight(rule.getWidth())-1,id+" shows full instructions without clipping");
                }
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
                check(((Label)root.lookup("#lastThrowLabel")).getText().contains("50 điểm"),"throw result displayed");
                check(((Label)root.lookup("#lastThrowScoreLabel")).getText().equals("50"),"persistent large score matches server result");
                res.setBust(true);res.setDartIndex(2);game.showThrow(res);
                check(((Label)root.lookup("#lastThrowLabel")).getText().contains("BUST"),"bust feedback displayed without changing game state");
                preview(root,"game-bust",1160,800);
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
