package services;

import protocol.dto.ThrowCoordinateRequest;
import java.util.Random;

public class BotService {
    private static final BotService instance = new BotService();
    private final Random random = new Random();
    
    private BotService() {}
    public static BotService getInstance() { return instance; }

    public void playTurn(String matchId) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                int rotation = random.nextInt(359) + 1;
                // apply rotation
                
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(1500);
                    // generate coordinate
                    double x = random.nextInt(200) - 100;
                    double y = random.nextInt(200) - 100;
                    ThrowCoordinateRequest req = new ThrowCoordinateRequest(matchId, 0, x, y);
                    GameService.getInstance().submitThrow(req, -1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
