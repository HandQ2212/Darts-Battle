import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import ui.ArenaBackdrop;
import ui.MatchResultPresentation;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

public class MatchResultPresentationTest {
    private static Stage stage;
    private static MatchResultPresentation presentation;
    private static StackPane root;

    private static <T> T fx(Callable<T> action) throws Exception {
        CompletableFuture<T> result=new CompletableFuture<>();
        Platform.runLater(()->{try{result.complete(action.call());}catch(Throwable error){result.completeExceptionally(error);}});
        return result.get(10,TimeUnit.SECONDS);
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);System.out.println("PASS "+message);}
    private static void awaitFx(BooleanSupplier condition,long timeoutMillis,String failure)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while(System.nanoTime()<deadline){if(fx(condition::getAsBoolean))return;Thread.sleep(25);}
        throw new AssertionError(failure);
    }

    public static void main(String[] args)throws Exception {
        CompletableFuture<Void> started=new CompletableFuture<>();
        Platform.startup(()->started.complete(null));started.get(10,TimeUnit.SECONDS);
        try{
            AtomicInteger skipped=new AtomicInteger(),cancelled=new AtomicInteger(),timed=new AtomicInteger();
            fx(()->{
                stage=new Stage();presentation=new MatchResultPresentation();root=new StackPane(new ArenaBackdrop(),presentation);
                Scene scene=new Scene(root,680,430);
                scene.getStylesheets().add(MatchResultPresentationTest.class.getResource("/views/theme.css").toExternalForm());
                stage.setScene(scene);stage.show();
                presentation.play(true,false,skipped::incrementAndGet);
                check(presentation.isPresenting()&&presentation.isVisible(),"victory reveal starts in the arena");
                Text title=(Text)presentation.lookup("#resultPresentationTitle");
                check("CHIẾN THẮNG".equals(title.getText()),"victory title text is correct");
                return null;
            });
            awaitFx(()->presentation.lookup("#resultPresentationTitle") instanceof Text title
                    &&title.getOpacity()>0.9&&title.getScaleX()>0.85,1800,"victory title animation did not become readable");
            fx(()->{
                if(System.getenv("DARTS_UI_PREVIEW")!=null)ClientUiSmokeTest.preview(root,"result-presentation-victory",680,430);
                Text revealTitle=(Text)presentation.lookup("#resultPresentationTitle");
                check(revealTitle.getFont().getSize()>40&&revealTitle.getScaleX()>0.85,"result title expands to readable size");
                ((Button)presentation.lookup("#resultPresentationSkip")).fire();
                check(skipped.get()==1&&!presentation.isVisible(),"skip ends reveal and calls completion once");
                ((Button)presentation.lookup("#resultPresentationSkip")).fire();
                check(skipped.get()==1,"repeated skip cannot run completion twice");

                presentation.play(true,false,cancelled::incrementAndGet);
                presentation.cancel();
                presentation.play(false,false,timed::incrementAndGet);
                Text title=(Text)presentation.lookup("#resultPresentationTitle");
                check("THUA CUỘC".equals(title.getText()),"defeat title text is correct");
                return null;
            });
            awaitFx(()->presentation.lookup("#resultPresentationTitle") instanceof Text title
                    &&title.getOpacity()>0.9&&title.getScaleX()>0.85,1800,"defeat title animation did not become readable");
            fx(()->{
                if(System.getenv("DARTS_UI_PREVIEW")!=null)ClientUiSmokeTest.preview(root,"result-presentation-defeat",680,430);
                check(((Text)presentation.lookup("#resultPresentationTitle")).getFont().getSize()>40,"defeat title also stays readable");
                return null;
            });
            Thread.sleep(2250);
            fx(()->{
                check(timed.get()==1&&!presentation.isPresenting()&&!presentation.isVisible(),"reveal auto-finishes after its display duration");
                check(cancelled.get()==0,"cancelled match callback cannot leak into the next result");
                stage.close();return null;
            });
            System.out.println("ALL RESULT PRESENTATION CHECKS PASSED");
        }finally{Platform.exit();}
    }
}
