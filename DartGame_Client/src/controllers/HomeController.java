package controllers;

import btl_ltm_n3.Main;
import static btl_ltm_n3.Main.socketHandler;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import java.util.Optional;
import javafx.scene.control.Button;

public class HomeController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private Label usernameLabel;
    @FXML
    private Button btnToggleMusic;

    private boolean isMusicPlaying = true;

    @FXML
    public void handleToggleMusic() {
        Main.toggleBackgroundMusic();
        

        // Đổi icon tương ứng trạng thái
        isMusicPlaying = !isMusicPlaying;
        btnToggleMusic.setText(isMusicPlaying ? "🎵" : "🔇");
    }
    @FXML
    public void initialize() {
        // Đặt background bằng hình ảnh
        Image bgImage = new Image(getClass().getResource("/images/background.jpg").toExternalForm());
        BackgroundImage bg = new BackgroundImage(
                bgImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, false)
        );
        rootPane.setBackground(new Background(bg));

        // Tạm hiển thị tên người dùng (sẽ thay bằng dữ liệu thực tế)
        setUsername(socketHandler.loginUser);
    }

    public void setUsername(String username) {
        usernameLabel.setText("👤 " + username);
    }

    public void handleLogout() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Xác nhận đăng xuất");
            alert.setHeaderText("Bạn có chắc chắn muốn đăng xuất?");
            alert.setContentText("Nhấn OK để đăng xuất, hoặc Cancel để quay lại.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    socketHandler.logout();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void handleStartGame() {
        try {
            Main.setRoot("choosemode");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleClickStartGameWithBot() {
        try {
            Main.setRoot("startgamewithbot");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleRanking() {
        try {
            Main.setRoot("ranking");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void handleClickMatchHistory() {
        try {
            Main.setRoot("match_history");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
