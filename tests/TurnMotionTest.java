import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.css.PseudoClass;
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
                leftTag.getStyleClass().add("turn-indicator");rightTag.getStyleClass().add("turn-indicator");
                Label leftName=new Label("Alice"),rightName=new Label("Bobby");
                HBox leftHeading=new HBox(5,leftName,leftTag),rightHeading=new HBox(5,rightName,rightTag);
                HBox.setHgrow(leftName,Priority.ALWAYS);HBox.setHgrow(rightName,Priority.ALWAYS);
                VBox left=new VBox(leftHeading,new Label("251")),right=new VBox(rightHeading,new Label("301"));left.setPrefSize(220,120);right.setPrefSize(220,120);
                left.getStyleClass().add("score-card");right.getStyleClass().addAll("score-card","opponent-card");
                HBox row=new HBox(450,left,right);Pane overlay=new Pane();StackPane root=new StackPane(row,overlay);
                Scene scene=new Scene(root,1040,300);scene.getStylesheets().add(TurnMotionTest.class.getResource("/views/theme.css").toExternalForm());root.resize(1040,300);root.applyCss();root.layout();
                TurnMotion motion=new TurnMotion(left,leftTag,right,rightTag,overlay);
                PseudoClass active=PseudoClass.getPseudoClass("active");
                left.pseudoClassStateChanged(active,true);motion.activate(true);root.applyCss();check(leftTag.isVisible()&&!rightTag.isVisible(),"initial turn is visible in player card");
                left.pseudoClassStateChanged(active,false);right.pseudoClassStateChanged(active,true);root.applyCss();
                motion.activate(false);check(!leftTag.isVisible()&&rightTag.isVisible()&&overlay.lookup("#flyingTurnTag")!=null,"turn marker travels to opponent");
                Label flying=(Label)overlay.lookup("#flyingTurnTag");check(flying.getWidth()>0&&flying.getHeight()>0,"moving marker has a visible size");
                check(overlay.isMouseTransparent(),"turn motion cannot block aiming");
                PauseTransition midway=new PauseTransition(Duration.millis(300));
                midway.setOnFinished(e->{try{
                    check(flying.getTranslateX()>10,"moving marker visibly travels across the screen");
                    right.pseudoClassStateChanged(active,false);left.pseudoClassStateChanged(active,true);root.applyCss();
                    motion.activate(true);check(leftTag.isVisible()&&!rightTag.isVisible(),"rapid handoff keeps latest owner");
                    PauseTransition wait=new PauseTransition(Duration.millis(750));
                    wait.setOnFinished(end->{try{
                        check(Math.abs(left.getScaleX()-1.15)<.001&&Math.abs(right.getScaleX()-1)<.001,"active card remains enlarged by fifteen percent");
                        check(Math.abs(leftTag.getFont().getSize()-16)<.1,"active turn marker uses prominent text");
                        check(overlay.lookup("#flyingTurnTag")==null&&leftTag.getOpacity()==1,"moving badge settles inside active card");
                        left.pseudoClassStateChanged(active,false);right.pseudoClassStateChanged(active,true);root.applyCss();
                        motion.activate(false);Label landing=(Label)overlay.lookup("#flyingTurnTag");
                        PauseTransition landingCheck=new PauseTransition(Duration.millis(540));
                        landingCheck.setOnFinished(checkEvent->{try{
                            var actual=landing.localToScene(landing.getLayoutBounds());var target=rightTag.localToScene(rightTag.getLayoutBounds());
                            double dx=(actual.getMinX()+actual.getMaxX()-target.getMinX()-target.getMaxX())/2;
                            double dy=(actual.getMinY()+actual.getMaxY()-target.getMinY()-target.getMaxY())/2;
                            check(Math.abs(dx)<8&&Math.abs(dy)<8,"moving marker lands at enlarged card position without snapping");
                            PauseTransition settle=new PauseTransition(Duration.millis(100));
                            settle.setOnFinished(settled->{try{
                                check(overlay.lookup("#flyingTurnTag")==null&&rightTag.getOpacity()==1,"landing marker settles into the destination tag");
                                motion.clear();check(!leftTag.isVisible()&&!rightTag.isVisible()&&left.getScaleX()==1&&right.getScaleX()==1,"match finish resets cards and marker");
                            }catch(Throwable t){failure.set(t);}finally{done.countDown();}});settle.play();
                        }catch(Throwable t){failure.set(t);done.countDown();}});landingCheck.play();
                    }catch(Throwable t){failure.set(t);done.countDown();}});wait.play();
                }catch(Throwable t){failure.set(t);done.countDown();}});midway.play();
            }catch(Throwable t){failure.set(t);done.countDown();}
        });
        if(!done.await(10,TimeUnit.SECONDS))throw new AssertionError("turn motion test timed out");
        Platform.exit();if(failure.get()!=null)throw new AssertionError(failure.get());
    }
}
