package models;

import java.util.Random;

public class Bot {
    private String name;
    private int difficultyLevel; // 1-10
    
    public Bot(String name, int difficultyLevel) {
        this.name = name;
        this.difficultyLevel = difficultyLevel;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(int difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    // Generates a random rotation between 1 and 359
    public int generateRotation() {
        Random random = new Random();
        return random.nextInt(359) + 1;
    }

    // Generates a target coordinate
    public double[] generateTargetCoordinate() {
        Random random = new Random();
        // Giả sử bảng phóng phi tiêu ở client có kích thước nhất định, 
        // Bot sẽ ném ngẫu nhiên hoặc có chiến thuật. Ở đây tạm thời random -150 đến 150
        double x = (random.nextDouble() * 300) - 150;
        double y = (random.nextDouble() * 300) - 150;
        return new double[]{x, y};
    }
}
