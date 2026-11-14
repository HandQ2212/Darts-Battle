package services;

import config.DartboardLayout;
import enums.BotDifficulty;
import java.util.Random;

/** Chooses one dart using the current score and rotation, independently of other darts. */
public final class BotAim {
    public record Target(double x,double y,int score,boolean random) {}
    private static final DartboardScorer SCORER=new DartboardScorer();
    private BotAim() {}
    public static Target choose(BotDifficulty difficulty,int remaining,int rotation,Random random) {
        if(difficulty==null||random==null||remaining<1)throw new IllegalArgumentException("Bot aim parameters invalid");
        if(random.nextDouble()<difficulty.randomProbability()){
            double x=-140+280*random.nextDouble(),y=-140+280*random.nextDouble();
            return new Target(x,y,SCORER.calculateDartScore(x,y,rotation),true);
        }
        Target best=optimal(remaining,rotation);
        return new Target(best.x(),best.y(),best.score(),false);
    }
    public static Target optimal(int remaining,int rotation) {
        if(remaining<1)throw new IllegalArgumentException("Remaining score invalid");
        Target best=new Target(0,0,0,false);
        if(remaining>=50)best=new Target(0,0,50,false);
        else if(remaining>=25)best=new Target(18,0,25,false);
        for(int i=0;i<DartboardLayout.BOARD_ORDER.length;i++){
            double radians=Math.toRadians(-90+i*18+rotation);
            for(double radius:new double[]{52,90,150}){
                double x=radius*Math.cos(radians),y=radius*Math.sin(radians);
                int score=SCORER.calculateDartScore(x,y,rotation);
                if(score<=remaining&&score>best.score())best=new Target(x,y,score,false);
            }
        }
        if(best.score()==0||SCORER.calculateDartScore(best.x(),best.y(),rotation)!=best.score())
            throw new IllegalStateException("No safe target for remaining score");
        return best;
    }
}
