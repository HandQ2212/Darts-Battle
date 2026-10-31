package ui;

import javafx.animation.*;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/** Visual turn handoff; the overlay never participates in game input. */
public final class TurnMotion {
    private final Region mine,other;
    private final Label myTag,otherTag;
    private final Pane overlay;
    private Boolean active;
    private Animation animation;
    private Label flying;
    public TurnMotion(Region mine,Label myTag,Region other,Label otherTag,Pane overlay){
        this.mine=mine;this.myTag=myTag;this.other=other;this.otherTag=otherTag;this.overlay=overlay;
        overlay.setMouseTransparent(true);
    }
    public void activate(boolean own){
        Boolean previous=active;stop();
        Region selected=own?mine:other,rest=own?other:mine;
        Label destination=own?myTag:otherTag,origin=own?otherTag:myTag;
        boolean handoff=previous!=null&&previous!=own&&origin.getScene()!=null&&destination.getScene()!=null;
        Point2D from=handoff?position(origin):null,to=handoff?position(destination):null;
        origin.setVisible(false);origin.setOpacity(1);destination.setVisible(true);destination.setOpacity(1);
        active=own;
        Timeline movement=new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(selected.scaleXProperty(),selected.getScaleX()),new KeyValue(selected.scaleYProperty(),selected.getScaleY()),
                new KeyValue(rest.scaleXProperty(),rest.getScaleX()),new KeyValue(rest.scaleYProperty(),rest.getScaleY()),
                new KeyValue(selected.rotateProperty(),0),new KeyValue(rest.rotateProperty(),0)),
            new KeyFrame(Duration.millis(190),
                new KeyValue(selected.rotateProperty(),own?-1.5:1.5,Interpolator.EASE_OUT),
                new KeyValue(rest.scaleXProperty(),1,Interpolator.EASE_BOTH),new KeyValue(rest.scaleYProperty(),1,Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.millis(550),
                new KeyValue(selected.scaleXProperty(),1.10,Interpolator.EASE_BOTH),new KeyValue(selected.scaleYProperty(),1.10,Interpolator.EASE_BOTH),
                new KeyValue(selected.rotateProperty(),0,Interpolator.EASE_BOTH)));
        if(handoff&&from!=null&&to!=null){
            destination.setOpacity(0);flying=new Label("Đến lượt");flying.setId("flyingTurnTag");flying.getStyleClass().add("turn-indicator");
            flying.setManaged(false);flying.setMouseTransparent(true);overlay.getChildren().add(flying);
            // Unmanaged controls are not laid out by Pane; size the moving label explicitly.
            flying.applyCss();flying.autosize();flying.relocate(from.getX(),from.getY());
            double dx=to.getX()-from.getX(),dy=to.getY()-from.getY();
            Timeline jump=new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(flying.translateXProperty(),0),new KeyValue(flying.translateYProperty(),0)),
                new KeyFrame(Duration.millis(275),new KeyValue(flying.translateXProperty(),dx/2,Interpolator.EASE_BOTH),new KeyValue(flying.translateYProperty(),dy/2-24,Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(550),new KeyValue(flying.translateXProperty(),dx,Interpolator.EASE_BOTH),new KeyValue(flying.translateYProperty(),dy,Interpolator.EASE_IN)));
            animation=new ParallelTransition(movement,jump);
        }else animation=movement;
        animation.setOnFinished(e->{removeFlying();destination.setOpacity(1);animation=null;});
        animation.play();
    }
    private Point2D position(Label label){
        Bounds b=label.localToScene(label.getBoundsInLocal());return b==null?null:overlay.sceneToLocal(b.getMinX(),b.getMinY());
    }
    private void removeFlying(){if(flying!=null){overlay.getChildren().remove(flying);flying=null;}}
    private void stop(){if(animation!=null){animation.stop();animation=null;}removeFlying();myTag.setOpacity(1);otherTag.setOpacity(1);mine.setRotate(0);other.setRotate(0);}
    public void clear(){
        stop();active=null;myTag.setVisible(false);otherTag.setVisible(false);
        mine.setScaleX(1);mine.setScaleY(1);other.setScaleX(1);other.setScaleY(1);
    }
}
