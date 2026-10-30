import btl_ltm_n3.Main;
import controllers.StartGameController;
import enums.MatchStatus;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import ui.DartThrowEffects;

/** Exercises real FX pulses: landing, sound timing, a fast turn change, and disposal. */
public class DartThrowEffectsTest {
    private static final CountDownLatch done=new CountDownLatch(1);
    private static final AtomicReference<Throwable> failure=new AtomicReference<>();
    private static ClientUiSmokeTest.FakeSocket socket;
    private static StartGameController controller;
    private static DartThrowEffects effects;
    private static Parent root;
    private static Group layer;
    private static int impacts;

    private static void later(int milliseconds,Runnable action) {
        PauseTransition pause=new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(e->{try {action.run();}catch(Throwable error){finish(error);}});pause.play();
    }
    private static void finish(Throwable error) {
        if(error!=null)failure.set(error);
        if(effects!=null)effects.dispose();if(controller!=null)controller.dispose();
        if(socket!=null)socket.shutdown();done.countDown();
    }
    private static void check(boolean value,String description) {ClientUiSmokeTest.check(value,description);}
    private static boolean popupHas(String value) {
        Group popup=(Group)layer.lookup(".score-popup");
        return popup!=null&&popup.getChildren().stream().anyMatch(node->node instanceof javafx.scene.text.Text text&&text.getText().contains(value));
    }
    private static void capture(String name) {
        try {ClientUiSmokeTest.preview(root,name,1160,800);}catch(Exception e){throw new RuntimeException(e);}
    }
    public static void main(String[] args)throws Exception {
        Platform.startup(()->{
            try {
                socket=new ClientUiSmokeTest.FakeSocket();Main.socketHandler=socket;
                socket.loginUser="Alice";socket.loggedInUserId=1;socket.competitor="Bobby";
                Main.gameViewState.begin(ClientUiSmokeTest.snap(1,1,0,MatchStatus.PLAYING,1L),1);
                FXMLLoader loader=new FXMLLoader(Main.class.getResource("/views/startgame.fxml"));root=loader.load();controller=loader.getController();
                layer=new Group();((Pane)root.lookup("#gamePane")).getChildren().add(layer);
                effects=new DartThrowEffects(layer,()->impacts++);effects.beginTurn();
                capture("dart-ready");effects.throwDart(225,200,true,50,false);
                check(impacts==0,"impact sound waits for flight");
                check(layer.lookup(".score-popup")==null,"score popup waits for impact");
                Group dart=(Group)layer.lookup(".dart-projectile");
                check(dart!=null&&dart.getChildren().size()>5,"dart has shaft, barrel and flights");
                Point2D initial=dart.localToParent(Point2D.ZERO);
                check(initial.distance(225,200)>50,"dart starts away from target");
                later(140,()->{capture("dart-flight");later(450,()->landed(dart));});
            } catch(Throwable error) {finish(error);}
        });
        if(!done.await(20,TimeUnit.SECONDS))throw new AssertionError("Dart animation timeout");
        Platform.exit();if(failure.get()!=null)throw new AssertionError(failure.get());
        System.out.println("ALL DART EFFECT CHECKS PASSED");
    }
    private static void landed(Group dart) {
        check(impacts==1,"one impact sound on landing");
        check(dart.localToParent(Point2D.ZERO).distance(225,200)<0.001,"tip lands exactly at confirmed coordinate");
        check(popupHas("50")&&popupHas("ĐIỂM"),"large popup shows confirmed dart score on landing");
        capture("dart-landed");
        effects.throwDart(300,240,false,20,true);effects.beginTurn();
        check(layer.lookupAll(".dart-projectile").size()==1,"turn change clears lodged darts and keeps in-flight last dart");
        later(500,()->{
            check(impacts==2,"third dart still lands and sounds after turn changes");
            check(popupHas("20")&&popupHas("BUST")&&popupHas("KHÔNG TRỪ"),"bust popup distinguishes scored hit from deducted points");
            check(layer.lookupAll(".score-popup").size()==1,"latest throw replaces previous popup");
            capture("dart-bust-popup");
            later(850,()->{
                check(layer.lookupAll(".dart-projectile").isEmpty(),"old-turn dart fades away");
                effects.throwDart(225,200,true,0,false);
                later(500,()->{
                    check(popupHas("0")&&popupHas("TRƯỢT"),"miss is clearly shown as zero");
                    effects.throwDart(225,200,true,30,false);effects.dispose();
                    later(450,()->{
                        check(impacts==3&&layer.getChildren().isEmpty(),"leaving view cancels animation, popup and pending sound");
                        check(socket.last==null,"animation sends no gameplay messages");integrationFeedback();
                    });
                });
            });
        });
    }
    private static void integrationFeedback() {
        ((javafx.scene.control.Button)root.lookup("#effectSoundButton")).fire();
        protocol.dto.ThrowResolvedResponse result=new protocol.dto.ThrowResolvedResponse();
        result.setMatchId("ui-match");result.setTurnId(1);result.setActorId(1L);result.setDartIndex(1);result.setX(110);result.setY(0);result.setDartScore(20);result.setRemainingScoreAfter(281);
        result.setSnapshot(ClientUiSmokeTest.snap(2,1,1,MatchStatus.PLAYING,1L));
        check(Main.gameViewState.applyThrow(result),"confirmed throw enters history");controller.showThrow(result);
        later(500,()->{
            check(root.lookup(".score-popup")!=null&&Main.gameViewState.history(true).get(0).deducted()==20,
                "real throw renderer shows popup and retains the result in history");
            capture("game-hit-feedback");finish(null);
        });
    }
}
