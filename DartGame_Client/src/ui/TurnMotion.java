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
        Region originCard=own?other:mine;
        Label destination=own?myTag:otherTag,origin=own?otherTag:myTag;
        boolean handoff=previous!=null&&previous!=own&&origin.getScene()!=null&&destination.getScene()!=null;
        Point2D from=handoff?positionAtScale(origin,originCard,originCard.getScaleX()):null;
        Point2D to=handoff?positionAtScale(destination,selected,1.15):null;
        origin.setVisible(false);origin.setOpacity(1);destination.setVisible(true);destination.setOpacity(1);
        active=own;
        Timeline movement=new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(selected.scaleXProperty(),selected.getScaleX()),new KeyValue(selected.scaleYProperty(),selected.getScaleY()),
                new KeyValue(rest.scaleXProperty(),rest.getScaleX()),new KeyValue(rest.scaleYProperty(),rest.getScaleY()),
                new KeyValue(selected.rotateProperty(),0),new KeyValue(rest.rotateProperty(),0)),
            new KeyFrame(Duration.millis(190),
                new KeyValue(selected.rotateProperty(),own?-1.8:1.8,Interpolator.EASE_OUT),
                new KeyValue(rest.scaleXProperty(),1,Interpolator.EASE_BOTH),new KeyValue(rest.scaleYProperty(),1,Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.millis(550),
                new KeyValue(selected.scaleXProperty(),1.15,Interpolator.EASE_BOTH),new KeyValue(selected.scaleYProperty(),1.15,Interpolator.EASE_BOTH),
                new KeyValue(selected.rotateProperty(),0,Interpolator.EASE_BOTH)));
        if(handoff&&from!=null&&to!=null){
            destination.setOpacity(0);flying=new Label("Đến lượt");flying.setId("flyingTurnTag");flying.getStyleClass().add("turn-indicator");
            flying.setManaged(false);flying.setMouseTransparent(true);overlay.getChildren().add(flying);
            // Unmanaged controls are not laid out by Pane; size the moving label explicitly.
            flying.applyCss();flying.autosize();
            double startScale=originCard.getScaleX(),endScale=1.15;
            flying.setScaleX(startScale);flying.setScaleY(startScale);
            Bounds startBounds=flying.localToParent(flying.getLayoutBounds());
            double startOffsetX=startBounds.getMinX()-flying.getLayoutX(),startOffsetY=startBounds.getMinY()-flying.getLayoutY();
            flying.setScaleX(endScale);flying.setScaleY(endScale);
            Bounds endBounds=flying.localToParent(flying.getLayoutBounds());
            double endOffsetX=endBounds.getMinX()-flying.getLayoutX(),endOffsetY=endBounds.getMinY()-flying.getLayoutY();
            flying.setScaleX(startScale);flying.setScaleY(startScale);
            flying.setLayoutX(from.getX()-startOffsetX);flying.setLayoutY(from.getY()-startOffsetY);
            double dx=to.getX()-from.getX()+startOffsetX-endOffsetX;
            double dy=to.getY()-from.getY()+startOffsetY-endOffsetY;
            Timeline jump=new Timeline(new KeyFrame(Duration.ZERO,
                    new KeyValue(flying.translateXProperty(),0),new KeyValue(flying.translateYProperty(),0),
                    new KeyValue(flying.scaleXProperty(),startScale),new KeyValue(flying.scaleYProperty(),startScale)),
                new KeyFrame(Duration.millis(275),
                    new KeyValue(flying.translateXProperty(),dx/2,Interpolator.EASE_BOTH),new KeyValue(flying.translateYProperty(),dy/2-24,Interpolator.EASE_OUT),
                    new KeyValue(flying.scaleXProperty(),(startScale+endScale)/2,Interpolator.EASE_BOTH),new KeyValue(flying.scaleYProperty(),(startScale+endScale)/2,Interpolator.EASE_BOTH),
                    new KeyValue(flying.rotateProperty(),own?1.2:-1.2,Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(550),
                    new KeyValue(flying.translateXProperty(),dx,Interpolator.EASE_BOTH),new KeyValue(flying.translateYProperty(),dy,Interpolator.EASE_IN),
                    new KeyValue(flying.scaleXProperty(),endScale,Interpolator.EASE_BOTH),new KeyValue(flying.scaleYProperty(),endScale,Interpolator.EASE_BOTH),
                    new KeyValue(flying.rotateProperty(),0,Interpolator.EASE_BOTH)));
            animation=new ParallelTransition(movement,jump);
        }else animation=movement;
        animation.setOnFinished(e->{removeFlying();destination.setOpacity(1);animation=null;});
        animation.play();
    }
    private Point2D positionAtScale(Label label,Region card,double targetScale){
        Bounds labelBounds=label.localToScene(label.getLayoutBounds());
        Bounds cardBounds=card.localToScene(card.getLayoutBounds());
        if(labelBounds==null||cardBounds==null)return null;
        Point2D labelTopLeft=overlay.sceneToLocal(labelBounds.getMinX(),labelBounds.getMinY());
        Point2D pivot=overlay.sceneToLocal((cardBounds.getMinX()+cardBounds.getMaxX())/2,(cardBounds.getMinY()+cardBounds.getMaxY())/2);
        double sx=targetScale/Math.max(0.01,card.getScaleX()),sy=targetScale/Math.max(0.01,card.getScaleY());
        return new Point2D(pivot.getX()+(labelTopLeft.getX()-pivot.getX())*sx,
                pivot.getY()+(labelTopLeft.getY()-pivot.getY())*sy);
    }
    private void removeFlying(){if(flying!=null){overlay.getChildren().remove(flying);flying=null;}}
    private void stop(){if(animation!=null){animation.stop();animation=null;}removeFlying();myTag.setOpacity(1);otherTag.setOpacity(1);mine.setRotate(0);other.setRotate(0);}
    public void clear(){
        stop();active=null;myTag.setVisible(false);otherTag.setVisible(false);
        mine.setScaleX(1);mine.setScaleY(1);other.setScaleX(1);other.setScaleY(1);
    }
}
