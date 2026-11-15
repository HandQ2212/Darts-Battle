package ui;

import btl_ltm_n3.Main;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** A short, skippable victory/defeat reveal confined to the match arena. */
public final class MatchResultPresentation extends StackPane {
    private static final double DURATION_MS = 2600;
    private static final int MAX_PARTICLES = 120;

    private static final class Spark {
        double x, y, vx, vy, age, lifetime, radius;
        final Color color;

        Spark(double x, double y, double vx, double vy, double lifetime, double radius, Color color) {
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.lifetime=lifetime;this.radius=radius;this.color=color;
        }
    }

    private final Rectangle dimmer = new Rectangle();
    private final Canvas canvas = new Canvas();
    private final Pane particleLayer = new Pane(canvas);
    private final Text title = new Text();
    private final Button skipButton = new Button("Bỏ qua  ›");
    private final List<Spark> sparks = new ArrayList<>();
    private final AnimationTimer particleTimer;

    private Timeline reveal;
    private Timeline soundFade;
    private MediaPlayer soundPlayer;
    private Runnable completion;
    private boolean presenting;
    private boolean victory;
    private boolean effectSoundEnabled = true;
    private long presentationGeneration;
    private long particleStart;
    private long lastFrame;
    private long nextBurst;
    private int burstNumber;
    private Long backgroundDuckToken;

    public MatchResultPresentation() {
        getStyleClass().add("result-presentation");
        setId("matchResultPresentation");
        setPickOnBounds(true);
        setFocusTraversable(true);

        dimmer.widthProperty().bind(widthProperty());
        dimmer.heightProperty().bind(heightProperty());
        dimmer.setMouseTransparent(true);
        particleLayer.setMinSize(0,0);
        particleLayer.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        particleLayer.setMouseTransparent(true);
        canvas.widthProperty().bind(particleLayer.widthProperty());
        canvas.heightProperty().bind(particleLayer.heightProperty());
        canvas.setMouseTransparent(true);

        title.setTextOrigin(VPos.CENTER);
        title.setId("resultPresentationTitle");
        title.getStyleClass().add("result-presentation-title");
        title.setTextAlignment(TextAlignment.CENTER);
        title.setStroke(Color.web("#4b2b13"));
        title.setStrokeWidth(2.4);
        title.setEffect(new DropShadow(18,Color.rgb(20,24,10,0.72)));
        title.setMouseTransparent(true);

        skipButton.getStyleClass().add("result-skip-button");
        skipButton.setId("resultPresentationSkip");
        skipButton.setAccessibleText("Bỏ qua hiệu ứng kết quả");
        skipButton.setTooltip(new javafx.scene.control.Tooltip("Bỏ qua hiệu ứng và xem kết quả"));
        skipButton.setOnAction(event->{event.consume();finishPresentation();});

        StackPane.setAlignment(skipButton,Pos.BOTTOM_RIGHT);
        StackPane.setMargin(skipButton,new Insets(0,18,16,0));
        getChildren().addAll(dimmer,particleLayer,title,skipButton);
        setVisible(false);
        setManaged(false);

        widthProperty().addListener((obs,oldValue,newValue)->fitTitle());
        addEventFilter(MouseEvent.MOUSE_CLICKED,event->{
            if(presenting){event.consume();finishPresentation();}
        });
        particleTimer=new AnimationTimer() {
            @Override public void handle(long now) {drawFrame(now);}
        };
    }

    public boolean isPresenting() { return presenting; }

    /** Starts one reveal. The completion callback runs once after the reveal or a skip. */
    public void play(boolean won, boolean playSound, Runnable onFinished) {
        cancel();
        presentationGeneration++;
        victory=won;
        effectSoundEnabled=playSound;
        completion=onFinished;
        presenting=true;
        burstNumber=0;
        sparks.clear();
        title.setText(won?"CHIẾN THẮNG":"THUA CUỘC");
        title.getStyleClass().removeAll("win","loss");
        title.getStyleClass().add(won?"win":"loss");
        title.setFill(won
                ? new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                    new Stop(0,Color.web("#fff7be")),new Stop(0.52,Color.web("#ffd04e")),new Stop(1,Color.web("#fff29a")))
                : new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                    new Stop(0,Color.web("#ffffff")),new Stop(0.52,Color.web("#cce8f5")),new Stop(1,Color.web("#a9cce0"))));
        fitTitle();
        setVisible(true);
        setManaged(true);
        toFront();
        skipButton.requestFocus();

        title.setOpacity(0);
        title.setScaleX(won?0.28:0.86);
        title.setScaleY(won?0.28:0.86);
        title.setRotate(won?-8:0);
        title.setTranslateY(won?22:-54);
        skipButton.setOpacity(0);
        dimmer.setOpacity(0);

        double shade=won?0.23:0.76;
        reveal=new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(title.opacityProperty(),0),
                        new KeyValue(dimmer.opacityProperty(),0),
                        new KeyValue(skipButton.opacityProperty(),0)),
                new KeyFrame(Duration.millis(520),
                        new KeyValue(title.opacityProperty(),1,Interpolator.EASE_OUT),
                        new KeyValue(title.translateYProperty(),0,Interpolator.EASE_OUT),
                        new KeyValue(title.scaleXProperty(),won?1.13:1,Interpolator.EASE_OUT),
                        new KeyValue(title.scaleYProperty(),won?1.13:1,Interpolator.EASE_OUT),
                        new KeyValue(title.rotateProperty(),won?1.3:0,Interpolator.EASE_OUT),
                        new KeyValue(dimmer.opacityProperty(),shade,Interpolator.EASE_OUT),
                        new KeyValue(skipButton.opacityProperty(),1,Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(760),
                        new KeyValue(title.scaleXProperty(),1,Interpolator.EASE_OUT),
                        new KeyValue(title.scaleYProperty(),1,Interpolator.EASE_OUT),
                        new KeyValue(title.rotateProperty(),0,Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(title.scaleXProperty(),won?1.035:1,Interpolator.EASE_OUT),
                        new KeyValue(title.scaleYProperty(),won?1.035:1,Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(1080),
                        new KeyValue(title.scaleXProperty(),1,Interpolator.EASE_BOTH),
                        new KeyValue(title.scaleYProperty(),1,Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(DURATION_MS)));
        long generation=presentationGeneration;
        reveal.setOnFinished(event->{if(generation==presentationGeneration)finishPresentation();});
        reveal.playFromStart();

        if(won){
            particleStart=System.nanoTime();lastFrame=0;nextBurst=particleStart+180_000_000L;
            particleTimer.start();
        }
        if(playSound)playResultSound(won?"win":"loose");
    }

    /** Turning effects off silences an active result immediately; turning them back on does not replay it. */
    public void setEffectSoundEnabled(boolean enabled) {
        effectSoundEnabled=enabled;
        if(!enabled)stopSoundImmediately();
    }

    /** Cancels old animations without invoking a stale result callback. */
    public void cancel() {
        presentationGeneration++;
        if(reveal!=null){reveal.stop();reveal=null;}
        particleTimer.stop();sparks.clear();clearCanvas();
        presenting=false;completion=null;
        stopSoundImmediately();
        setVisible(false);setManaged(false);
        title.setOpacity(0);dimmer.setOpacity(0);skipButton.setOpacity(0);
    }

    private void fitTitle() {
        if(title.getText()==null||title.getText().isEmpty())return;
        double available=Math.max(180,getWidth()*0.82);
        Text measure=new Text(title.getText());
        for(double size=88;size>=32;size-=2){
            Font font=Font.font("System",FontWeight.EXTRA_BOLD,size);
            measure.setFont(font);
            if(measure.getLayoutBounds().getWidth()<=available){
                title.setFont(font);title.setStyle("-fx-font-family: 'System'; -fx-font-size: "+size+"px; -fx-font-weight: bold;");return;
            }
        }
        title.setFont(Font.font("System",FontWeight.EXTRA_BOLD,32));title.setStyle("-fx-font-family: 'System'; -fx-font-size: 32px; -fx-font-weight: bold;");
    }

    private void drawFrame(long now) {
        if(!presenting||!victory)return;
        if(now>=nextBurst){
            double side=(burstNumber++%2==0)?0.16:0.84;
            double x=Math.max(26,Math.min(getWidth()-26,getWidth()*side));
            double y=getHeight()*(0.16+ThreadLocalRandom.current().nextDouble(0.08));
            spawnBurst(x,y);
            nextBurst=now+360_000_000L;
        }
        double dt=lastFrame==0?1.0/60:Math.min(0.04,(now-lastFrame)/1_000_000_000.0);
        lastFrame=now;
        GraphicsContext graphics=canvas.getGraphicsContext2D();
        graphics.clearRect(0,0,canvas.getWidth(),canvas.getHeight());
        Iterator<Spark> iterator=sparks.iterator();
        while(iterator.hasNext()){
            Spark spark=iterator.next();spark.age+=dt;
            if(spark.age>=spark.lifetime){iterator.remove();continue;}
            spark.x+=spark.vx*dt;spark.y+=spark.vy*dt;spark.vy+=118*dt;
            double alpha=Math.max(0,1-spark.age/spark.lifetime);
            graphics.setGlobalAlpha(alpha*0.20);
            graphics.setFill(spark.color);
            graphics.fillOval(spark.x-spark.radius*2.2,spark.y-spark.radius*2.2,spark.radius*4.4,spark.radius*4.4);
            graphics.setGlobalAlpha(alpha);
            graphics.fillOval(spark.x-spark.radius,spark.y-spark.radius,spark.radius*2,spark.radius*2);
        }
        graphics.setGlobalAlpha(1);
    }

    private void spawnBurst(double x,double y) {
        if(sparks.size()>=MAX_PARTICLES)return;
        ThreadLocalRandom random=ThreadLocalRandom.current();
        Color[] palette={Color.web("#ffe66d"),Color.web("#ffae45"),Color.web("#fff4c2"),Color.web("#ff7a3d"),Color.web("#c5ef83")};
        int count=Math.min(28,MAX_PARTICLES-sparks.size());
        for(int i=0;i<count;i++){
            double angle=random.nextDouble(Math.PI*2);
            double speed=random.nextDouble(50,155);
            double lifetime=random.nextDouble(0.82,1.35);
            sparks.add(new Spark(x,y,Math.cos(angle)*speed,Math.sin(angle)*speed,
                    lifetime,random.nextDouble(2.0,3.8),palette[random.nextInt(palette.length)]));
        }
    }

    private void clearCanvas() {
        GraphicsContext graphics=canvas.getGraphicsContext2D();
        graphics.clearRect(0,0,canvas.getWidth(),canvas.getHeight());
    }

    private void finishPresentation() {
        if(!presenting)return;
        presenting=false;
        if(reveal!=null){reveal.stop();reveal=null;}
        particleTimer.stop();sparks.clear();clearCanvas();
        setVisible(false);setManaged(false);
        Runnable callback=completion;completion=null;
        fadeSoundOut();
        if(callback!=null)callback.run();
    }

    private void playResultSound(String name) {
        try{
            var resource=getClass().getResource("/musics/"+name+".mp3");
            if(resource==null)return;
            MediaPlayer player=new MediaPlayer(new Media(resource.toExternalForm()));
            soundPlayer=player;player.setVolume(0.92);
            player.setOnReady(()->{
                if(soundPlayer!=player||!presenting||!effectSoundEnabled)return;
                backgroundDuckToken=Main.duckBackgroundMusic();
                player.play();
            });
            player.setOnEndOfMedia(()->releaseSound(player));
            player.setOnError(()->releaseSound(player));
        }catch(RuntimeException ignored){stopSoundImmediately();}
    }

    private void fadeSoundOut() {
        if(soundPlayer==null){restoreBackgroundMusic();return;}
        if(soundFade!=null){soundFade.stop();soundFade=null;}
        MediaPlayer player=soundPlayer;
        soundFade=new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(player.volumeProperty(),player.getVolume())),
                new KeyFrame(Duration.millis(150),new KeyValue(player.volumeProperty(),0,Interpolator.EASE_OUT)));
        soundFade.setOnFinished(event->releaseSound(player));
        soundFade.playFromStart();
    }

    private void stopSoundImmediately() {
        if(soundFade!=null){soundFade.stop();soundFade=null;}
        MediaPlayer player=soundPlayer;soundPlayer=null;
        if(player!=null){player.setOnEndOfMedia(null);player.setOnError(null);player.stop();player.dispose();}
        restoreBackgroundMusic();
    }

    private void releaseSound(MediaPlayer player) {
        if(soundPlayer!=player)return;
        if(soundFade!=null){soundFade.stop();soundFade=null;}
        soundPlayer=null;player.setOnEndOfMedia(null);player.setOnError(null);player.dispose();
        restoreBackgroundMusic();
    }

    private void restoreBackgroundMusic() {
        if(backgroundDuckToken==null)return;
        Main.restoreBackgroundMusic(backgroundDuckToken);
        backgroundDuckToken=null;
    }
}
