import btl_ltm_n3.Main;
import controllers.StartGameController;
import enums.MatchStatus;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import java.util.concurrent.*;

/** Real FX pulses verify zoom, multiple turns, input gating and cancellation. */
public class AutomaticRotationTest {
    private static ClientUiSmokeTest.FakeSocket socket;
    private static StartGameController controller;
    private static Parent root;
    private static Rotate rotation;
    private static Scale scale;
    private static Label aim;
    private static double maximumAngle;
    private static <T> T fx(Callable<T> action) throws Exception {
        CompletableFuture<T> result=new CompletableFuture<>();
        Platform.runLater(()->{try{result.complete(action.call());}catch(Throwable error){result.completeExceptionally(error);}});
        return result.get(10,TimeUnit.SECONDS);
    }
    private static void check(boolean condition,String message) {ClientUiSmokeTest.check(condition,message);}
    public static void main(String[] args) throws Exception {
        CompletableFuture<Void> ready=new CompletableFuture<>();Platform.startup(()->ready.complete(null));ready.get(10,TimeUnit.SECONDS);
        try {
            fx(()->{
                socket=new ClientUiSmokeTest.FakeSocket();Main.socketHandler=socket;
                socket.loginUser="Alice";socket.loggedInUserId=1;socket.competitor="Bobby";
                Main.gameViewState.begin(ClientUiSmokeTest.snap(1,1,0,MatchStatus.WAITING_ROTATION,1L),1);
                FXMLLoader loader=new FXMLLoader(Main.class.getResource("/views/startgame.fxml"));root=loader.load();controller=loader.getController();
                ClientUiSmokeTest.preview(root,"auto-rotation-start",1160,800);
                Group board=(Group)((Pane)root.lookup("#gamePane")).getChildren().get(0);
                rotation=(Rotate)board.getTransforms().get(0);scale=(Scale)board.getTransforms().get(1);
                rotation.angleProperty().addListener((observable,old,value)->maximumAngle=Math.max(maximumAngle,value.doubleValue()));
                aim=(Label)root.lookup("#aimStatusLabel");
                check(aim.isDisabled(),"aim locked at automatic rotation start");
                socket.last=null;ClientUiSmokeTest.clickBoard(root.lookup("#gamePane"),javafx.scene.input.MouseButton.PRIMARY);check(socket.last==null,"board clicks during rotation cannot submit a throw");
                return null;
            });
            Thread.sleep(500);
            fx(()->{
                check(socket.mediaJoins==1,"PvP automatically sends exactly one media join");
                check(scale.getX()>1.15&&scale.getY()>1.15,"board enlarges before spinning");
                var playing=ClientUiSmokeTest.snap(2,1,0,MatchStatus.PLAYING,1L);
                Main.gameViewState.apply(playing);controller.rotationConfirmed(playing);controller.render(playing);
                check(aim.isDisabled(),"early server readiness does not skip animation or unlock aiming");
                return null;
            });
            Thread.sleep(1000);
            fx(()->{check(maximumAngle>360,"board rotates multiple full circles");ClientUiSmokeTest.preview(root,"auto-rotation-zoom",1160,800);return null;});
            Thread.sleep(1700);
            fx(()->{
                check(Math.abs(scale.getX()-1)<0.001&&Math.abs(scale.getY()-1)<0.001,"board returns to original size");
                check(Math.abs(rotation.getAngle()-90)<0.001,"board stops at server-confirmed angle");
                check(maximumAngle>=1080,"rotation completes at least three full circles");
                check(!aim.isDisabled(),"aiming enabled only after animation completes");
                check(socket.last==null,"animation sends no manual rotation or throw requests");
                ClientUiSmokeTest.preview(root,"auto-rotation-ready",1160,800);
                var other=ClientUiSmokeTest.snap(3,2,0,MatchStatus.WAITING_ROTATION,2L);Main.gameViewState.apply(other);controller.render(other);
                check(aim.isDisabled(),"opponent turn also rotates with local aiming locked");return null;
            });
            Thread.sleep(500);
            fx(()->{check(scale.getX()>1.15,"opponent sees the same zoom effect");controller.dispose();check(scale.getX()==1,"leaving during rotation restores scale");return null;});
            Thread.sleep(2900);
            fx(()->{controller.handleGameAction();check(socket.last==null&&aim.isDisabled(),"disposed rotation never restarts aiming or sends commands");return null;});
            System.out.println("ALL AUTOMATIC ROTATION CHECKS PASSED");
        } finally {
            fx(()->{if(controller!=null)controller.dispose();if(socket!=null)socket.shutdown();return null;});Platform.exit();
        }
    }
}
