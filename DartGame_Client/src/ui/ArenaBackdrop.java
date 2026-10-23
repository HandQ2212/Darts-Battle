package ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.*;

/** Downloaded 2D forest scenery. Decorative only, outside the fixed aiming surface. */
public final class ArenaBackdrop extends Region {
    private static final Image FOREST=new Image(ArenaBackdrop.class.getResource("/assets/art/forest-background.png").toExternalForm());
    private final Canvas canvas=new Canvas();
    private boolean showMount=true;
    public ArenaBackdrop() {getChildren().add(canvas);setMouseTransparent(true);setMinSize(0,0);}
    public void setShowMount(boolean value) {showMount=value;canvas.setWidth(0);requestLayout();}
    @Override protected void layoutChildren() {
        double w=getWidth(),h=getHeight();
        if(w<=0||h<=0||(canvas.getWidth()==w&&canvas.getHeight()==h))return;
        canvas.setWidth(w);canvas.setHeight(h);
        GraphicsContext g=canvas.getGraphicsContext2D();
        double sourceWidth=FOREST.getWidth(),sourceHeight=FOREST.getHeight();
        double scale=Math.max(w/sourceWidth,h/sourceHeight),sw=w/scale,sh=h/scale;
        g.drawImage(FOREST,(sourceWidth-sw)/2,(sourceHeight-sh)/2,sw,sh,0,0,w,h);
        if(showMount) {
            g.setFill(new RadialGradient(0,0,w/2,h/2,Math.max(w,h)*0.56,false,CycleMethod.NO_CYCLE,
                new Stop(0,Color.web("#fff4af",0.3)),new Stop(1,Color.TRANSPARENT)));
            g.fillRect(0,0,w,h);
        }
    }
}
