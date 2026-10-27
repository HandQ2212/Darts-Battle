package controllers;

import btl_ltm_n3.Main;
import static btl_ltm_n3.Main.socketHandler;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
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
        btnToggleMusic.setText(isMusicPlaying ? "Nhạc: bật" : "Nhạc: tắt");
    }
    @FXML
    public void initialize() {
        setUsername(socketHandler.loginUser);
    }

    public void setUsername(String username) {
        usernameLabel.setText(username);
    }

    @FXML public void handleCredits() {
        Alert credits=new Alert(Alert.AlertType.INFORMATION);
        ui.GameTheme.style(credits);
        credits.setTitle("Nguồn hình ảnh");credits.setHeaderText("Hình ảnh và font chữ");
        credits.setContentText("Nền rừng: Cartoon Forest 2D Backgrounds — CraftPix.net\n"
            +"https://opengameart.org/content/cartoon-forest-2d-backgrounds\n"
            +"Giấy phép: OGA-BY 3.0 — https://static.opengameart.org/OGA-BY-3.0.txt\n"
            +"Ảnh gốc không sửa; chỉ co giãn/cắt khung khi hiển thị.\n\n"
            +"Font Lilita One — Juan Montoreano, SIL OFL 1.1 (Google Fonts).\n"
            +"Icon Crosshair Pack — Kenney.nl, CC0.\n\n"
            +"Không sử dụng ảnh tạo bằng AI. Giấy phép được đóng kèm client.");
        credits.show();
    }

    public void handleLogout() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            ui.GameTheme.style(alert);
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
            Main.setRoot("chooseOpponent");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleClickStartGameWithBot() {
        try {
            socketHandler.createBotMatch();
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
