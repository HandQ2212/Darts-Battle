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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import models.User;
import models.GameViewState;

import java.io.IOException;
import java.util.ArrayList;

public class Main extends Application {

    private static Stage primaryStage;
    public static SocketHandler socketHandler;
    public static GameViewState gameViewState = new GameViewState();
    private static Scene scene;

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
        primaryStage = stage;
        socketHandler = new SocketHandler();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
        scene = new Scene(loader.load(), 1080, 740);
        stage.setScene(scene);
        stage.setTitle("Darts Battle");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        ui.GameTheme.style(alert);
        alert.setTitle("Xác nhận thoát");
        alert.setHeaderText("Bạn có chắc muốn thoát ứng dụng không?");
        alert.setContentText("Chọn OK để thoát, Cancel để ở lại.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                socketHandler.shutdown();
                stopBackgroundMusic(); // 🧩 Dừng nhạc khi thoát
                Platform.exit();
                System.exit(0);
            }
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
        scene.setRoot(root);
        if (primaryStage != null) {
            primaryStage.setMinWidth(fxml.equals("startgame") ? 1040 : 1000);
            primaryStage.setMinHeight(fxml.equals("startgame") ? 780 : 700);
            primaryStage.sizeToScene();
        }
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
