import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;
import ui.TurnMotion;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class TurnMotionTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS "+message);}
    public static void main(String[] args)throws Exception{
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
        Platform.startup(()->{
            try{
                Label leftTag=new Label("Đến lượt"),rightTag=new Label("Đến lượt");
                VBox left=new VBox(leftTag),right=new VBox(rightTag);left.setPrefSize(220,120);right.setPrefSize(220,120);
                HBox row=new HBox(450,left,right);Pane overlay=new Pane();StackPane root=new StackPane(row,overlay);
                new Scene(root,1040,300);root.resize(1040,300);root.applyCss();root.layout();
                TurnMotion motion=new TurnMotion(left,leftTag,right,rightTag,overlay);
                motion.activate(true);check(leftTag.isVisible()&&!rightTag.isVisible(),"initial turn is visible in player card");
                motion.activate(false);check(!leftTag.isVisible()&&rightTag.isVisible()&&overlay.lookup("#flyingTurnTag")!=null,"turn marker travels to opponent");
                Label flying=(Label)overlay.lookup("#flyingTurnTag");check(flying.getWidth()>0&&flying.getHeight()>0,"moving marker has a visible size");
                check(overlay.isMouseTransparent(),"turn motion cannot block aiming");
                PauseTransition midway=new PauseTransition(Duration.millis(300));
                midway.setOnFinished(e->{try{
                    check(flying.getTranslateX()>10,"moving marker visibly travels across the screen");
                    motion.activate(true);check(leftTag.isVisible()&&!rightTag.isVisible(),"rapid handoff keeps latest owner");
                    PauseTransition wait=new PauseTransition(Duration.millis(750));
                    wait.setOnFinished(end->{try{
                        check(Math.abs(left.getScaleX()-1.10)<.001&&Math.abs(right.getScaleX()-1)<.001,"active card remains enlarged");
                        check(overlay.lookup("#flyingTurnTag")==null&&leftTag.getOpacity()==1,"moving badge settles inside active card");
                        motion.clear();check(!leftTag.isVisible()&&!rightTag.isVisible()&&left.getScaleX()==1&&right.getScaleX()==1,"match finish resets cards and marker");
                    }catch(Throwable t){failure.set(t);}finally{done.countDown();}});wait.play();
                }catch(Throwable t){failure.set(t);done.countDown();}});midway.play();
            }catch(Throwable t){failure.set(t);done.countDown();}
        });
        if(!done.await(10,TimeUnit.SECONDS))throw new AssertionError("turn motion test timed out");
        Platform.exit();if(failure.get()!=null)throw new AssertionError(failure.get());
    }
}
