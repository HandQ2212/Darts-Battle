package ui;

import config.DartboardLayout;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.*;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Flat 2D menu illustration, painted locally; no gameplay state or generated image assets. */
public final class MenuArtwork extends Region {
    private final Canvas canvas=new Canvas();
    public MenuArtwork() {getChildren().add(canvas);setMouseTransparent(true);setMinSize(0,0);}
    @Override protected void layoutChildren() {
        double w=getWidth(),h=getHeight();
        if(canvas.getWidth()==w&&canvas.getHeight()==h)return;
        canvas.setWidth(w);canvas.setHeight(h);
        GraphicsContext g=canvas.getGraphicsContext2D();
        g.clearRect(0,0,w,h);double r=Math.min(w*0.37,h*0.37),cx=w*0.51,cy=h*0.49;
        g.save();g.translate(cx,cy);g.rotate(-12);
        g.setFill(Color.web("#3a4d20",0.8));g.fillOval(-r-8,-r+2,2*r+16,2*r+16);
        g.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#fbd075")),new Stop(0.5,Color.web("#d59442")),new Stop(1,Color.web("#a86428"))));
        g.fillOval(-r-5,-r-5,2*r+10,2*r+10);
        g.setFill(Color.web("#e0a346"));g.fillOval(-r,-r,2*r,2*r);
        double face=r*0.81;
        for(int i=0;i<20;i++) {
            double start=99-i*18;
            g.setFill(Color.web(i%2==0?"#ffe392":"#fff1bc"));
            g.fillArc(-face,-face,face*2,face*2,start,-18,ArcType.ROUND);
        }
        for(double[] ring:new double[][]{{0.81,0.71},{0.51,0.41}}) {
            for(int i=0;i<20;i++) {
                g.setStroke(Color.web(i%2==0?"#ff9956":"#70b85b"));g.setLineWidth(r*(ring[0]-ring[1]));
                double rr=r*(ring[0]+ring[1])/2;
                g.strokeArc(-rr,-rr,rr*2,rr*2,99-i*18,-18,ArcType.OPEN);
            }
        }
        g.setFill(Color.web("#70b85b"));g.fillOval(-r*.13,-r*.13,r*.26,r*.26);
        g.setFill(Color.web("#ff9956"));g.fillOval(-r*.06,-r*.06,r*.12,r*.12);
        g.setFont(Font.font("Lilita One",FontWeight.BOLD,Math.max(11,r*.095)));g.setFill(Color.web("#74451e"));
        for(int i=0;i<20;i++) {
            double a=Math.toRadians(i*18-90);String n=""+DartboardLayout.BOARD_ORDER[i];
            g.fillText(n,r*.91*Math.cos(a)-n.length()*r*.022,r*.91*Math.sin(a)+r*.035);
        }
        g.restore();
        dart(g,cx+r*.10,cy-r*.14,r/110,Color.web("#ffac36"));
        dart(g,cx+r*.70,cy+r*.52,r/170,Color.web("#55c0d5"));
        g.setFill(Color.web("#fff2ba"));g.setFont(Font.font("Lilita One",FontWeight.BOLD,13));
        g.fillText("NGẮM CHUẨN · NÉM TRÚNG!",Math.max(18,cx-r),h-42);
    }
    private void dart(GraphicsContext g,double x,double y,double scale,Color color) {
        g.save();g.translate(x,y);g.rotate(12);g.scale(scale,scale);
        g.setStroke(Color.web("#ffda83",0.24));g.setLineWidth(1);
        for(int i=0;i<3;i++)g.strokeLine(80+i*6,40+i*6,150+i*8,75+i*6);
        g.setStroke(Color.web("#dceaf4"));g.setLineWidth(2);g.strokeLine(0,0,16,8);
        g.setFill(Color.web("#9eb4c5"));g.fillPolygon(new double[]{14,18,42,39},new double[]{4,3,17,22},4);
        g.setStroke(Color.web("#f0f8ff"));g.setLineWidth(2);g.strokeLine(40,19,63,31);
        g.setFill(color);g.fillPolygon(new double[]{58,57,86,71},new double[]{27,10,30,38},4);
        g.setFill(color.darker());g.fillPolygon(new double[]{61,86,82,69},new double[]{29,37,51,37},4);
        g.restore();
    }
}
