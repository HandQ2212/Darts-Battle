import enums.BotDifficulty;
import services.BotAim;
import services.DartboardScorer;
import java.util.Random;

/** Deterministic selection and geometry checks, independent of probability sampling noise. */
public class BotAimTest {
    private static final class Sequence extends Random {
        private final double[] values;private int index;
        Sequence(double... values){this.values=values;}
        @Override public double nextDouble(){return values[Math.min(index++,values.length-1)];}
    }
    private static void check(boolean condition,String text){if(!condition)throw new AssertionError(text);}
    public static void main(String[] args){
        DartboardScorer scorer=new DartboardScorer();
        for(BotDifficulty d:BotDifficulty.values()){
            double p=d.randomProbability();
            check(BotAim.choose(d,301,0,new Sequence(Math.max(0,p-0.01),0.999,0.999)).random(),d+" random threshold");
            if(p<1)check(!BotAim.choose(d,301,0,new Sequence(p)).random(),d+" optimal threshold");
        }
        BotAim.Target miss=BotAim.choose(BotDifficulty.EASY,301,0,new Sequence(0,0.999,0.999));
        check(miss.score()==0&&miss.x()<140&&miss.y()<140,"random coordinates retain their bounds and can miss");
        for(int rotation:new int[]{0,1,45,90,179,270,359})for(int remaining=1;remaining<=301;remaining++){
            int expected=remaining>=50?50:remaining>=25?25:0;
            for(int n=1;n<=20;n++)for(int multiplier=1;multiplier<=3;multiplier++)if(n*multiplier<=remaining)expected=Math.max(expected,n*multiplier);
            BotAim.Target target=BotAim.optimal(remaining,rotation);
            check(target.score()==expected&&scorer.calculateDartScore(target.x(),target.y(),rotation)==expected,
                "optimal target for "+remaining+" at rotation "+rotation);
        }
        check(BotAim.optimal(50,87).score()==50&&BotAim.optimal(59,87).score()==57,"checkout and non-checkout targets");
        System.out.println("ALL BOT AIM CHECKS PASSED");
    }
}
