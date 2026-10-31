package ui;

import javafx.animation.*;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.Duration;

/** Animates a confirmed score without changing the authoritative game state. */
public final class ScoreEffect {
    private final Text score;
    private final StackPane row;
    private final Label feedback=new Label();
    private Animation count,feedbackAnimation,emphasis;
    private int target;
    public ScoreEffect(Text score,StackPane row){
        this.score=score;this.row=row;feedback.setId("scoreFeedback");feedback.getStyleClass().add("score-feedback");
        feedback.setMouseTransparent(true);feedback.setVisible(false);
        StackPane.setAlignment(score,Pos.CENTER_LEFT);StackPane.setAlignment(feedback,Pos.CENTER_RIGHT);
        row.getChildren().addAll(score,feedback);
    }
    public void sync(int value){
        if(count!=null&&count.getStatus()==Animation.Status.RUNNING&&target==value)return;
        stopCount();target=value;score.setText(Integer.toString(value));
    }
    public void play(int value,int points,boolean bust){
        int from;try{from=Integer.parseInt(score.getText());}catch(NumberFormatException e){from=value;}
        stopCount();target=value;
        if(!bust&&from!=value){
            SimpleDoubleProperty number=new SimpleDoubleProperty(from);
            number.addListener((o,old,n)->score.setText(Integer.toString((int)Math.round(n.doubleValue()))));
            Timeline counter=new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(number,from)),
                    new KeyFrame(Duration.millis(500),new KeyValue(number,value,Interpolator.EASE_OUT)));
            counter.setOnFinished(e->{score.setText(Integer.toString(target));count=null;});
            count=counter;counter.play();
        }else score.setText(Integer.toString(value));
        if(feedbackAnimation!=null)feedbackAnimation.stop();
        feedback.getStyleClass().removeAll("score-feedback-bust","score-feedback-miss","score-feedback-win");
        feedback.setText(bust?"BUST":points==0?"Trượt":"−"+points);
        if(bust)feedback.getStyleClass().add("score-feedback-bust");
        else if(points==0)feedback.getStyleClass().add("score-feedback-miss");
        else if(value==0)feedback.getStyleClass().add("score-feedback-win");
        feedback.setVisible(true);feedback.setOpacity(1);feedback.setTranslateY(4);
        stopEmphasis();
        if(bust){
            Timeline shake=new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(row.translateXProperty(),0)),
                new KeyFrame(Duration.millis(70),new KeyValue(row.translateXProperty(),3)),
                new KeyFrame(Duration.millis(140),new KeyValue(row.translateXProperty(),-3)),
                new KeyFrame(Duration.millis(210),new KeyValue(row.translateXProperty(),0)));
            emphasis=shake;shake.play();
        }else if(points>0){
            ScaleTransition pulse=new ScaleTransition(Duration.millis(180),score);pulse.setFromX(1);pulse.setFromY(1);
            pulse.setToX(value==0?1.12:1.06);pulse.setToY(value==0?1.12:1.06);pulse.setAutoReverse(true);pulse.setCycleCount(2);
            emphasis=pulse;pulse.play();
        }
        TranslateTransition rise=new TranslateTransition(Duration.millis(750),feedback);rise.setFromY(4);rise.setToY(-18);
        FadeTransition fade=new FadeTransition(Duration.millis(750),feedback);fade.setFromValue(1);fade.setToValue(0);
        ParallelTransition animation=new ParallelTransition(rise,fade);
        animation.setOnFinished(e->{feedback.setVisible(false);feedbackAnimation=null;});
        feedbackAnimation=animation;animation.play();
    }
    private void stopCount(){if(count!=null){count.stop();count=null;}}
    private void stopEmphasis(){if(emphasis!=null){emphasis.stop();emphasis=null;}row.setTranslateX(0);score.setScaleX(1);score.setScaleY(1);}
    public void reset(int value){stopCount();stopEmphasis();if(feedbackAnimation!=null){feedbackAnimation.stop();feedbackAnimation=null;}feedback.setVisible(false);target=value;score.setText(Integer.toString(value));}
    public void dispose(){stopCount();stopEmphasis();if(feedbackAnimation!=null)feedbackAnimation.stop();}
}
