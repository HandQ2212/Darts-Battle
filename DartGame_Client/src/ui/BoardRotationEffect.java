package ui;

import config.TurnRotation;
import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/** Cosmetic turns around the scoring center; never changes layout or scoring coordinates. */
public final class BoardRotationEffect {
    private final Rotate rotation;
    private final Scale scale;
    private Timeline animation;
    private int target;

    public BoardRotationEffect(Node board,double centerX,double centerY) {
        rotation=new Rotate(0,centerX,centerY);
        scale=new Scale(1,1,centerX,centerY);
        board.getTransforms().addAll(rotation,scale);
    }
    public boolean isRunning() { return animation!=null; }
    public void setAngle(int degrees) {
        if(!isRunning())rotation.setAngle(degrees);
    }
    public void play(int degrees,Runnable completed) {
        cancel();target=degrees;
        double from=((rotation.getAngle()%360)+360)%360;
        double delta=((degrees-from)%360+360)%360;
        rotation.setAngle(from);
        animation=new Timeline(
            new KeyFrame(Duration.ZERO,new KeyValue(scale.xProperty(),1),new KeyValue(scale.yProperty(),1),new KeyValue(rotation.angleProperty(),from)),
            new KeyFrame(Duration.millis(350),new KeyValue(scale.xProperty(),1.18,Interpolator.EASE_OUT),new KeyValue(scale.yProperty(),1.18,Interpolator.EASE_OUT),new KeyValue(rotation.angleProperty(),from)),
            new KeyFrame(Duration.millis(2350),new KeyValue(rotation.angleProperty(),from+1080+delta,Interpolator.EASE_OUT),new KeyValue(scale.xProperty(),1.18),new KeyValue(scale.yProperty(),1.18)),
            new KeyFrame(Duration.millis(TurnRotation.DURATION_MILLIS),new KeyValue(rotation.angleProperty(),from+1080+delta),new KeyValue(scale.xProperty(),1,Interpolator.EASE_BOTH),new KeyValue(scale.yProperty(),1,Interpolator.EASE_BOTH))
        );
        animation.setOnFinished(event->{animation=null;rotation.setAngle(target);completed.run();});
        animation.play();
    }
    public void cancel() {
        if(animation!=null){animation.stop();animation.setOnFinished(null);animation=null;}
        scale.setX(1);scale.setY(1);
    }
}
