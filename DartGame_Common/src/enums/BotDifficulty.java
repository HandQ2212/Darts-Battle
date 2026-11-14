package enums;

/** Probability of using the existing random-coordinate strategy for each dart. */
public enum BotDifficulty {
    EASY("Dễ",1.0), MEDIUM("Trung bình",0.75), HARD("Khó",0.5), EXTREME("Cực khó",0.25);
    private final String label;
    private final double randomProbability;
    BotDifficulty(String label,double randomProbability){this.label=label;this.randomProbability=randomProbability;}
    public String label(){return label;}
    public double randomProbability(){return randomProbability;}
    @Override public String toString(){return label;}
}
