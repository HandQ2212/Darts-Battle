package ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javafx.animation.*;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

/** Cosmetic only. The local origin of each dart is its tip at the server's hit coordinate. */
public final class DartThrowEffects {
    private final Group layer, lodged=new Group(), flying=new Group(), feedback=new Group();
    private Animation scoreAnimation;
    private final Runnable impactSound;
    private final Set<Animation> animations=new HashSet<>();
    private long generation;
    private boolean disposed;

    public DartThrowEffects(Group layer,Runnable impactSound) {
        this.layer=layer;this.impactSound=impactSound;
        layer.setMouseTransparent(true);
        layer.setClip(new Rectangle(0,0,450,400));
        layer.getChildren().addAll(lodged,flying,feedback);
    }

    /** Clear the previous turn, but let its last dart finish landing and playing its impact. */
    public void beginTurn() {generation++;lodged.getChildren().clear();}

    public void throwDart(double x,double y,boolean own) {
        throwDart(x,y,own,null,false);
    }

    public void throwDart(double x,double y,boolean own,Integer score,boolean bust) {
        if(disposed)return;
        long turn=generation;
        Color accent=Color.web(own?"#87d7bc":"#f0ae6c");
        Group dart=createDart(accent);dart.getStyleClass().add("dart-projectile");
        dart.setLayoutX(x);dart.setLayoutY(y);
        Translate flight=new Translate(110+(450-x)*0.35,Math.max(140,440-y));
        Scale perspective=new Scale(1.9,1.9,0,0);
        Rotate angle=new Rotate(-28,0,0);
        dart.getTransforms().addAll(flight,angle,perspective);
        flying.getChildren().add(dart);
        Timeline animation=new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(flight.xProperty(),110+(450-x)*0.35),
                new KeyValue(flight.yProperty(),Math.max(140,440-y)),
                new KeyValue(perspective.xProperty(),1.9),new KeyValue(perspective.yProperty(),1.9),
                new KeyValue(angle.angleProperty(),-28)),
            new KeyFrame(Duration.millis(280),event->{
                impactSound.run();impact(x,y,accent);
                if(score!=null)showScore(x,y,own,score,bust);
            },new KeyValue(flight.xProperty(),0,Interpolator.EASE_IN),
                new KeyValue(flight.yProperty(),0,Interpolator.EASE_IN),
                new KeyValue(perspective.xProperty(),1,Interpolator.EASE_OUT),
                new KeyValue(perspective.yProperty(),1,Interpolator.EASE_OUT),
                new KeyValue(angle.angleProperty(),-12,Interpolator.EASE_OUT)),
            new KeyFrame(Duration.millis(315),new KeyValue(angle.angleProperty(),-7)),
            new KeyFrame(Duration.millis(370),new KeyValue(angle.angleProperty(),-12)));
        animation.setOnFinished(event->{
            animations.remove(animation);
            if(turn==generation) {
                flying.getChildren().remove(dart);lodged.getChildren().add(dart);
            } else {
                // A third dart may arrive immediately before TURN_STARTED. Keep
                // that landing visible briefly without delaying the next turn.
                FadeTransition fade=new FadeTransition(Duration.millis(650),dart);
                fade.setFromValue(1);fade.setToValue(0);fade.setDelay(Duration.millis(180));
                fade.setOnFinished(e->{flying.getChildren().remove(dart);animations.remove(fade);});
                animations.add(fade);fade.play();
            }
        });
        animations.add(animation);animation.play();
    }

    private void showScore(double x,double y,boolean own,int score,boolean bust) {
        if(scoreAnimation!=null){scoreAnimation.stop();animations.remove(scoreAnimation);}
        feedback.getChildren().clear();
        Group bubble=new Group();bubble.getStyleClass().add("score-popup");
        Rectangle plaque=new Rectangle(-87,-49,174,98);plaque.setArcWidth(28);plaque.setArcHeight(28);
        plaque.setFill(Color.web(bust?"#d95635":score==0?"#6c8862":"#ff900c"));
        plaque.setStroke(Color.web("#75451f"));plaque.setStrokeWidth(3);
        Text heading=centered(own?"BẠN VỪA NÉM":"ĐỐI THỦ VỪA NÉM",11,-29,Color.web("#fff6da"));
        Text number=centered(String.valueOf(score),46,17,Color.web("#fff9db"));
        number.setFont(Font.font("Lilita One",46));number.setX(-number.getLayoutBounds().getWidth()/2);
        number.setStroke(Color.web("#744020"));number.setStrokeWidth(1);
        Text caption=centered(bust?"BUST · KHÔNG TRỪ ĐIỂM":score==0?"TRƯỢT BẢNG":"ĐIỂM",10,36,Color.web("#fff9db"));
        bubble.getChildren().addAll(plaque,heading,number,caption);
        bubble.setLayoutX(Math.max(92,Math.min(358,x)));bubble.setLayoutY(Math.max(94,Math.min(320,y-100)));
        bubble.setEffect(new DropShadow(5,0,4,Color.color(0.24,0.15,0.04,0.25)));
        Scale pop=new Scale(0.65,0.65,0,0);Translate rise=new Translate();bubble.getTransforms().addAll(rise,pop);bubble.setOpacity(0);
        feedback.getChildren().add(bubble);
        Timeline animation=new Timeline(
            new KeyFrame(Duration.ZERO,new KeyValue(pop.xProperty(),0.65),new KeyValue(pop.yProperty(),0.65),new KeyValue(bubble.opacityProperty(),0)),
            new KeyFrame(Duration.millis(90),new KeyValue(pop.xProperty(),1.10,Interpolator.EASE_OUT),new KeyValue(pop.yProperty(),1.10,Interpolator.EASE_OUT),new KeyValue(bubble.opacityProperty(),1)),
            new KeyFrame(Duration.millis(180),new KeyValue(pop.xProperty(),1),new KeyValue(pop.yProperty(),1)),
            new KeyFrame(Duration.millis(1200),new KeyValue(bubble.opacityProperty(),1)),
            new KeyFrame(Duration.millis(1600),new KeyValue(bubble.opacityProperty(),0),new KeyValue(rise.yProperty(),-36,Interpolator.EASE_OUT)));
        animation.setOnFinished(event->{feedback.getChildren().remove(bubble);animations.remove(animation);if(scoreAnimation==animation)scoreAnimation=null;});
        scoreAnimation=animation;animations.add(animation);animation.play();
    }

    private Text centered(String value,double size,double y,Color color) {
        Text text=new Text(value);text.setFont(Font.font("System",FontWeight.BOLD,size));text.setFill(color);
        text.setX(-text.getLayoutBounds().getWidth()/2);text.setY(y);return text;
    }

    private void impact(double x,double y,Color accent) {
        Circle ring=new Circle(x,y,4,Color.TRANSPARENT);
        ring.setStroke(accent);ring.setStrokeWidth(2);flying.getChildren().add(ring);
        Timeline pulse=new Timeline(new KeyFrame(Duration.millis(260),
            new KeyValue(ring.radiusProperty(),22,Interpolator.EASE_OUT),new KeyValue(ring.opacityProperty(),0)));
        pulse.setOnFinished(event->{flying.getChildren().remove(ring);animations.remove(pulse);});
        animations.add(pulse);pulse.play();
    }

    private Group createDart(Color accent) {
        Line tip=new Line(0,0,14,10);tip.setStroke(Color.web("#f4f6ef"));tip.setStrokeWidth(2);
        Polygon barrel=new Polygon(11,6,16,6,37,23,34,29,29,28);
        barrel.setFill(Color.web("#b9c8cb"));barrel.setStroke(Color.web("#35505a"));barrel.setStrokeWidth(0.8);
        Line shine=new Line(16,9,33,23);shine.setStroke(Color.web("#edf3ec"));shine.setStrokeWidth(1.6);
        Line shaft=new Line(35,26,54,41);shaft.setStroke(Color.web("#e2e9df"));shaft.setStrokeWidth(3);
        Polygon upperFlight=new Polygon(47,36,45,21,69,39,57,45);
        upperFlight.setFill(accent);upperFlight.setStroke(Color.web("#23434c"));upperFlight.setStrokeWidth(0.8);
        Polygon lowerFlight=new Polygon(49,39,70,46,64,58,53,46);
        lowerFlight.setFill(accent.darker());lowerFlight.setStroke(Color.web("#23434c"));lowerFlight.setStrokeWidth(0.8);
        Line seam=new Line(49,37,65,47);seam.setStroke(accent.brighter());seam.setStrokeWidth(1.4);
        Group dart=new Group(tip,shaft,upperFlight,lowerFlight,barrel,shine,seam);
        for(int i=0;i<4;i++) {
            Line grip=new Line(19+i*3,13+i*2.5,17+i*3,16+i*2.5);
            grip.setStroke(Color.web("#59717a"));grip.setStrokeWidth(1);dart.getChildren().add(grip);
        }
        dart.setEffect(new DropShadow(4,2,3,Color.color(0.05,0.12,0.15,0.32)));
        return dart;
    }

    public void dispose() {
        disposed=true;
        for(Animation animation:new ArrayList<>(animations))animation.stop();
        animations.clear();lodged.getChildren().clear();flying.getChildren().clear();feedback.getChildren().clear();scoreAnimation=null;
        layer.getChildren().clear();
    }
}
