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
        volatile Message last;
        volatile int mediaJoins;
        java.util.function.BiConsumer<Boolean,String> listener;
        @Override public void setConnectionListener(java.util.function.BiConsumer<Boolean,String> listener) {
            this.listener=listener;if(listener!=null)listener.accept(false,"Đang kết nối...");
        }
        @Override public void ensureConnected(Runnable ready) {}
        @Override public void sendData(Message m) {if(m.getType().name().startsWith("MEDIA_")){if(m.getType()==MessageType.MEDIA_JOIN)mediaJoins++;return;}last=m;}
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS "+message);}
    static void clickBoard(Node target,javafx.scene.input.MouseButton button) {
        javafx.event.Event.fireEvent(target,new javafx.scene.input.MouseEvent(
            javafx.scene.input.MouseEvent.MOUSE_CLICKED,100,100,100,100,button,1,
            false,false,false,false,false,false,false,false,button==javafx.scene.input.MouseButton.SECONDARY,true,
            new javafx.scene.input.PickResult(target,100,100)));
    }
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
                MatchSnapshot initial=snap(1,1,0,MatchStatus.PLAYING,1L);Main.gameViewState.begin(initial,1);
                FXMLLoader loader=new FXMLLoader(Main.class.getResource("/views/startgame.fxml"));Parent root=loader.load();StartGameController game=loader.getController();
                Main.notifications().setPage(root);new Scene(Main.notifications().root(),1024,741);
                check(root.lookup("#communicationTabs")==null&&root.lookup("#joinCallButton")==null,"chat and video use the three-column layout without a join button");
                ((Button)root.lookup("#effectSoundButton")).fire();
                Label start=(Label)root.lookup("#aimStatusLabel");
                check(root.lookup("#rotateButton")==null&&root.lookup("#angleInput")==null,"manual rotation controls removed");
                preview(root,"game-rotation",1024,741);
                javafx.scene.layout.Region surface=(javafx.scene.layout.Region)root.lookup("#gamePane");
                check(surface.getWidth()==450&&surface.getHeight()==400,"board coordinate surface remains 450 by 400");
                check(root.lookup("#myTurnTag").isVisible()&&!root.lookup("#otherTurnTag").isVisible(),"only the active player's turn is shown");
                check(root.lookupAll(".score-value").size()==2,"both remaining scores visible");
                preview(root,"game-minimum",1024,741);
                for(String id:new String[]{"#gamePane","#leftColumn","#rightColumn","#aimStatusLabel","#chatInput","#localVideoFrame","#remoteVideoFrame","#microphoneButton","#cameraButton"})fits(root,id);
                var left=(javafx.scene.layout.Region)root.lookup("#leftColumn");var right=(javafx.scene.layout.Region)root.lookup("#rightColumn");
                check(Math.abs(left.getWidth()-right.getWidth())<1,"side columns have equal width");
                for(String videoId:new String[]{"#localVideoFrame","#remoteVideoFrame"}){
                    var video=(javafx.scene.layout.Region)root.lookup(videoId);
                    check(Math.abs(video.getHeight()-video.getWidth()*9/16)<2,videoId+" remains 16:9");
                }
                var boardBounds=surface.localToScene(surface.getBoundsInLocal());
                check(Math.abs((boardBounds.getMinX()+boardBounds.getMaxX())/2-root.getLayoutBounds().getWidth()/2)<2,"dartboard remains centered");
                check(((Button)root.lookup("#microphoneButton")).getAccessibleText().equals("Bật mic")&&((Button)root.lookup("#cameraButton")).getAccessibleText().equals("Bật camera"),"microphone and camera start off");
                Button mic=(Button)root.lookup("#microphoneButton");mic.fire();check(mic.getAccessibleText().startsWith("Hủy"),"device intent waits during negotiation");mic.fire();check(mic.getAccessibleText().equals("Bật mic"),"pending device intent can be canceled");
                MatchSnapshot playing=snap(2,1,0,MatchStatus.PLAYING,1L);check(Main.gameViewState.apply(playing),"new version accepted");game.rotationConfirmed(playing);
                check(!start.isDisabled(),"playing enables aiming without angle input");
                check(!Main.gameViewState.apply(playing)&&!Main.gameViewState.apply(initial),"duplicate/stale snapshots ignored");
                check(root.lookup("#startButton")==null,"stop buttons replaced by board click guidance");
                socket.last=null;
                clickBoard(root.lookup("#gamePane"),javafx.scene.input.MouseButton.SECONDARY);
                check(socket.last==null&&start.getText().contains("X"),"right click cannot stop aiming");
                clickBoard(root.lookup("#chatInput"),javafx.scene.input.MouseButton.PRIMARY);
                check(socket.last==null&&start.getText().contains("X"),"chat clicks cannot stop aiming");
                clickBoard(root.lookup("#gamePane"),javafx.scene.input.MouseButton.PRIMARY);
                check(socket.last==null&&!start.getText().contains("X"),"first board click locks X without submitting a throw");
                clickBoard(root.lookup("#gamePane"),javafx.scene.input.MouseButton.PRIMARY);
                check(socket.last.getPayload() instanceof ThrowCoordinateRequest,"second board click locks Y and submits coordinates");
                Message submitted=socket.last;
                clickBoard(root.lookup("#gamePane"),javafx.scene.input.MouseButton.PRIMARY);
                check(socket.last==submitted,"extra clicks while pending cannot submit another throw");
                ThrowCoordinateRequest request=(ThrowCoordinateRequest)socket.last.getPayload();check(request.getDartIndex()==1&&request.getTurnId()==1&&request.getX()<0&&request.getY()<0,"relative coordinates and dart identity");
                check(start.isDisabled(),"one in-flight throw");
                ThrowResolvedResponse res=new ThrowResolvedResponse();res.setMatchId("ui-match");res.setTurnId(1);res.setActorId(1L);res.setDartIndex(1);res.setX(0);res.setY(0);res.setDartScore(50);res.setSnapshot(snap(3,1,1,MatchStatus.PLAYING,1L));
                res.setRemainingScoreAfter(251);check(Main.gameViewState.applyThrow(res),"throw added to history");game.showThrow(res);check(!start.isDisabled(),"next dart enabled after result");
                check(!Main.gameViewState.applyThrow(res),"duplicate throw is not recorded twice");
                ((Button)root.lookup("#myHistoryButton")).fire();
                check(root.isDisabled()&&Main.notifications().root().lookup("#historyScroll")!=null,"history modal blocks game input");
                socket.last=null;game.handleGameAction();check(socket.last==null,"aiming does not submit while history is open");
                ThrowResolvedResponse bust=new ThrowResolvedResponse();bust.setMatchId("ui-match");bust.setTurnId(1);bust.setActorId(1L);bust.setDartIndex(2);bust.setDartScore(100);bust.setBust(true);bust.setRemainingScoreAfter(251);bust.setSnapshot(snap(4,1,2,MatchStatus.PLAYING,1L));
                check(Main.gameViewState.applyThrow(bust),"bust is recorded");game.showThrow(bust);
                var turn=Main.gameViewState.history(true).get(0);check(turn.deducted()==50&&turn.remaining()==251&&turn.darts().get(1).bust(),"bust contributes zero and preserves remaining score");
                Main.notifications().root().applyCss();Main.notifications().root().layout();
                check(Main.notifications().root().lookupAll(".history-row").stream().filter(n->n instanceof javafx.scene.layout.VBox).count()==2,"history displays both players and refreshes while open");
                preview(Main.notifications().root(),"game-history",1024,741);
                ((Button)Main.notifications().root().lookup("#customDialogClose")).fire();check(!root.isDisabled()&&!start.isDisabled(),"closing history resumes valid aiming");
                ((Button)root.lookup("#deviceSettingsButton")).fire();
                check(Main.notifications().root().lookup("#deviceSettingsPanel")!=null&&root.isDisabled(),"device choices are in a modal");
                ((Button)Main.notifications().root().lookup("#customDialogClose")).fire();
                ((Button)root.lookup("#deviceSettingsButton")).fire();
                check(Main.notifications().root().lookup("#deviceSettingsPanel")!=null,"device settings can be reopened");
                ((Button)Main.notifications().root().lookup("#customDialogClose")).fire();
                preview(root,"game-bust",1160,800);
                MatchSnapshot other=snap(5,2,0,MatchStatus.WAITING_ROTATION,2L);Main.gameViewState.apply(other);game.render(other);
                check(start.isDisabled(),"opponent turn locks aiming");
                check(!root.lookup("#myTurnTag").isVisible()&&root.lookup("#otherTurnTag").isVisible(),"turn indicator moves to opponent");
                socket.last=null;clickBoard(root.lookup("#gamePane"),javafx.scene.input.MouseButton.PRIMARY);
                check(socket.last==null,"board clicks on opponent turn are ignored");
                ((Button)root.lookup("#otherHistoryButton")).fire();
                MatchSnapshot finished=new MatchSnapshot("ui-match",MatchMode.PVP,1,"Alice",2L,"Bobby",1L,ActorType.HUMAN,3,1,0,0,281,MatchStatus.FINISHED,6,1L,ActorType.HUMAN,"ZERO_SCORE",0);
                Main.gameViewState.apply(finished);game.finish(finished);
                check(Main.notifications().root().lookup("#historyScroll")==null&&Main.notifications().root().lookup("#notificationDialog")!=null,"match result replaces an open history modal");
                Main.notifications().setPage(root);
                game.dispose();socket.last=null;game.handleGameAction();check(socket.last==null,"disposed controller sends nothing");
                Main.gameViewState.begin(initial,1);check(Main.gameViewState.history(true).isEmpty(),"new match clears turn history");
                models.GameViewState isolated=new models.GameViewState();isolated.begin(initial,1);
                ThrowResolvedResponse opponent=new ThrowResolvedResponse();opponent.setMatchId("ui-match");opponent.setTurnId(2);opponent.setActorId(2L);opponent.setDartIndex(1);opponent.setDartScore(20);opponent.setRemainingScoreAfter(281);opponent.setSnapshot(snap(2,2,1,MatchStatus.PLAYING,2L));
                check(isolated.applyThrow(opponent)&&isolated.history(false).get(0).deducted()==20,"opponent results are stored separately");
                ThrowResolvedResponse winning=new ThrowResolvedResponse();winning.setMatchId("ui-match");winning.setTurnId(3);winning.setActorId(1L);winning.setDartIndex(1);winning.setDartScore(50);winning.setRemainingScoreAfter(0);winning.setSnapshot(snap(3,3,1,MatchStatus.FINISHED,1L));
                check(isolated.applyThrow(winning)&&isolated.history(true).get(0).remaining()==0,"winning dart remains in match history");
                MatchSnapshot bot=new MatchSnapshot("bot-match",MatchMode.PVE,1,"Alice",null,"BOT",1L,ActorType.HUMAN,1,0,0,301,301,MatchStatus.PLAYING,1,null,null,null,System.currentTimeMillis()+120000);
                Main.gameViewState.begin(bot,1);FXMLLoader botLoader=new FXMLLoader(Main.class.getResource("/views/startgame.fxml"));Parent botRoot=botLoader.load();StartGameController botGame=botLoader.getController();
                check(!botRoot.lookup("#mediaControls").isManaged()&&!botRoot.lookup("#chatComposer").isManaged(),"bot match hides media and chat input");
                check(((Label)botRoot.lookup("#remoteVideoPlaceholder")).getGraphic()!=null,"bot has an avatar");botGame.dispose();
                System.out.println("ALL UI SMOKE CHECKS PASSED");
            } catch(Throwable e){failure.set(e);} finally {socket.shutdown();done.countDown();}
        });
        if(!done.await(30,TimeUnit.SECONDS))throw new AssertionError("UI timeout");
        Platform.exit();if(failure.get()!=null)throw new AssertionError(failure.get());
    }
}
