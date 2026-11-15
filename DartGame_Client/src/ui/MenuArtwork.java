package ui;

import config.DartboardLayout;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.FontWeight;

/** Flat 2D menu illustration, painted locally; no gameplay state or generated image assets. */
public final class MenuArtwork extends Region {
    private final Canvas canvas=new Canvas();
    private final Label slogan=new Label("NGẮM CHUẨN · NÉM TRÚNG!");
    public MenuArtwork() {
        slogan.getStyleClass().add("menu-art-slogan");slogan.setId("menuArtworkSlogan");slogan.setWrapText(false);
        slogan.setMaxWidth(Double.MAX_VALUE);slogan.setAlignment(javafx.geometry.Pos.CENTER);
        getChildren().addAll(canvas,slogan);setMouseTransparent(true);setMinSize(0,0);
    }
    @Override protected void layoutChildren() {
        double w=getWidth(),h=getHeight();
        double sloganHeight=Math.min(38,Math.max(30,h*0.16));
        double drawHeight=Math.max(0,h-sloganHeight-4);
        canvas.relocate(0,0);slogan.resizeRelocate(0,drawHeight+4,w,sloganHeight);
        if(canvas.getWidth()==w&&canvas.getHeight()==drawHeight)return;
        canvas.setWidth(w);canvas.setHeight(drawHeight);
        GraphicsContext g=canvas.getGraphicsContext2D();g.clearRect(0,0,w,drawHeight);
        double r=Math.min((w-24)/2,(drawHeight-24)/2);
        if(r<=0)return;
        double cx=w/2,cy=drawHeight/2;
        g.save();g.translate(cx,cy);
        g.setFill(Color.web("#3a4d20",0.8));g.fillOval(-r-8,-r+2,2*r+16,2*r+16);
        g.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#fbd075")),new Stop(0.5,Color.web("#d59442")),new Stop(1,Color.web("#a86428"))));
        g.fillOval(-r-5,-r-5,2*r+10,2*r+10);
        g.setFill(Color.web("#e0a346"));g.fillOval(-r,-r,2*r,2*r);
        double face=r*0.81;
        for(int i=0;i<20;i++) {
            double start=99-i*18;
            g.setFill(Color.web(i%2==0?"#ffe392":"#fff1bc"));
            fillSector(g,face,start,-18);
        }
        for(double[] ring:new double[][]{{0.81,0.71},{0.51,0.41}}) {
            for(int i=0;i<20;i++) {
                g.setFill(Color.web(i%2==0?"#ff9956":"#70b85b"));
                fillRingSector(g,r*ring[0],r*ring[1],99-i*18,-18);
            }
        }
        g.setFill(Color.web("#70b85b"));g.fillOval(-r*.13,-r*.13,r*.26,r*.26);
        g.setFill(Color.web("#ff9956"));g.fillOval(-r*.06,-r*.06,r*.12,r*.12);
        Font numberFont=Font.font("Lilita One",FontWeight.BOLD,Math.max(11,r*.095));g.setFont(numberFont);g.setFill(Color.web("#74451e"));
        for(int i=0;i<20;i++) {
            double a=Math.toRadians(i*18-90);String n=""+DartboardLayout.BOARD_ORDER[i];
            Text measured=new Text(n);measured.setFont(numberFont);var bounds=measured.getLayoutBounds();
            double x=r*.91*Math.cos(a),y=r*.91*Math.sin(a);
            g.fillText(n,x-(bounds.getMinX()+bounds.getMaxX())/2,y-(bounds.getMinY()+bounds.getMaxY())/2);
        }
        // Keep the decorative darts inside the same measured bounds as the board and its shadow.
        dart(g,-r*.42,-r*.12,r/280,Color.web("#ffac36"));
        dart(g,r*.08,r*.18,r/330,Color.web("#55c0d5"));
        g.restore();
    }
    private void fillSector(GraphicsContext g,double radius,double start,double extent) {
        g.beginPath();g.moveTo(0,0);appendArc(g,radius,start,extent);g.closePath();g.fill();
    }
    private void fillRingSector(GraphicsContext g,double outer,double inner,double start,double extent) {
        // Build both arcs from the same angle samples so neighboring 18-degree wedges share edges.
        double first=Math.toRadians(start);
        g.beginPath();g.moveTo(outer*Math.cos(first),-outer*Math.sin(first));appendArc(g,outer,start,extent);
        for(int step=12;step>=0;step--) {
            double angle=Math.toRadians(start+extent*step/12.0);
            g.lineTo(inner*Math.cos(angle),-inner*Math.sin(angle));
        }
        g.closePath();g.fill();
    }
    private void appendArc(GraphicsContext g,double radius,double start,double extent) {
        for(int step=0;step<=12;step++) {
            double angle=Math.toRadians(start+extent*step/12.0);
            g.lineTo(radius*Math.cos(angle),-radius*Math.sin(angle));
        }
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
