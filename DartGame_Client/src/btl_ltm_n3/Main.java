package btl_ltm_n3;

import controllers.ChooseOpponentController;
import controllers.MatchHistoryController;
import controllers.RankingController;
import controllers.SocketHandler;
import controllers.StartGameController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import models.User;
import models.GameViewState;

import java.io.IOException;
import java.util.ArrayList;

public class Main extends Application {

    public static SocketHandler socketHandler;
    public static GameViewState gameViewState = new GameViewState();
    private static Scene scene;
    private static ui.Notifications notifications;
    public static ui.Notifications notifications() {
        if(notifications==null)notifications=new ui.Notifications();
        return notifications;
    }

    public static ArrayList<User> listOnlineUser = new ArrayList<>();
    public static ArrayList<String> leaderboardData = new ArrayList<>();

    public static ChooseOpponentController chooseOpponentController;
    public static StartGameController startGameController;
    public static RankingController rankingController;
    public static MatchHistoryController matchHistoryController;
    // 🎵 Biến lưu trình phát nhạc
    private static MediaPlayer backgroundPlayer;

    @Override
    public void start(Stage stage) throws Exception {
        socketHandler = new SocketHandler();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
        notifications=new ui.Notifications();
        notifications.setPage(loader.load());
        scene = new Scene(notifications.root(), 1080, 780);
        stage.setScene(scene);
        stage.setTitle("Darts Battle");
        // Use the same minimum on every page so navigation never resizes the window.
        stage.setMinWidth(1040);
        stage.setMinHeight(780);
        stage.show();

        // Phát nhạc nền
        playBackgroundMusic();

        // Sự kiện đóng app
        stage.setOnCloseRequest(event -> {
            event.consume();
            handleExitConfirmation();
        });

    }

    private void handleExitConfirmation() {
        notifications().confirm("Rời sân hôm nay?","Nếu đang trong trận, thoát ứng dụng sẽ bị tính là bỏ cuộc.","Thoát game",()->{
                socketHandler.shutdown();
                stopBackgroundMusic(); // 🧩 Dừng nhạc khi thoát
                notifications().dispose();
                Platform.exit();
                System.exit(0);
        });
    }

    public static void setRoot(String fxml) throws IOException {
        socketHandler.setConnectionListener(null);
        if (startGameController != null) startGameController.dispose();
        startGameController = null;
        chooseOpponentController = null;
        rankingController = null;
        matchHistoryController = null;
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/views/" + fxml + ".fxml"));
        Parent root = loader.load();

        if (fxml.equals("chooseOpponent") || fxml.equals("choosecomponent")) {
            chooseOpponentController = loader.getController();
        }
        if (fxml.equals("startgame")) {
            startGameController = loader.getController();
        }
        if (fxml.equals("ranking")) {
            rankingController = loader.getController();
        }
        if (fxml.equals("match_history")) {
            matchHistoryController = loader.getController();
        }
        notifications().setPage(root);
    }

    // ======================================================
    // NHẠC NỀN: HÀM BẬT / TẮT
    // ======================================================

    public static void playBackgroundMusic() {
        try {
            if (backgroundPlayer == null) {
                String path = Main.class.getResource("/musics/background_bb_0.mp3").toExternalForm();
                Media media = new Media(path);
                backgroundPlayer = new MediaPlayer(media);
                backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE); // lặp vô hạn
                backgroundPlayer.setVolume(0.3); // âm lượng vừa phải
            }
            backgroundPlayer.play();
        } catch (Exception e) {
            System.out.println("Không thể phát nhạc nền: " + e.getMessage());
        }
    }

    public static void stopBackgroundMusic() {
        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
        }
    }

    public static void toggleBackgroundMusic() {
        if (backgroundPlayer == null) return;

        if (backgroundPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            backgroundPlayer.pause();
        } else {
            backgroundPlayer.play();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
