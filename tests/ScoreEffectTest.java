import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.Duration;
import ui.ScoreEffect;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ScoreEffectTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS "+message);}
    public static void main(String[] args)throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
        Platform.startup(()->{
            try{
                Text number=new Text();StackPane row=new StackPane();ScoreEffect effect=new ScoreEffect(number,row);
                Label feedback=(Label)row.lookup("#scoreFeedback");
                effect.sync(301);effect.play(251,50,false);effect.sync(251);
                check(number.getText().equals("301")&&feedback.getText().equals("−50"),"score begins at previous value and shows deduction");
                effect.play(201,50,false);effect.sync(201);
                PauseTransition wait=new PauseTransition(Duration.millis(700));
                wait.setOnFinished(e->{
                    try{
                        check(number.getText().equals("201"),"rapid updates finish at latest server score");
                        effect.play(201,60,true);check(number.getText().equals("201")&&feedback.getText().equals("BUST"),"bust keeps score unchanged");
                        effect.play(201,0,false);check(feedback.getText().equals("Trượt"),"miss is displayed");
                        effect.play(0,1,false);effect.reset(301);
                        check(number.getText().equals("301")&&!feedback.isVisible(),"new match clears animation");
                        effect.dispose();
                    }catch(Throwable t){failure.set(t);}finally{done.countDown();}
                });wait.play();
            }catch(Throwable t){failure.set(t);done.countDown();}
        });
        if(!done.await(10,TimeUnit.SECONDS))throw new AssertionError("Animation test timed out");
        Platform.exit();if(failure.get()!=null)throw new AssertionError(failure.get());
    }
}
