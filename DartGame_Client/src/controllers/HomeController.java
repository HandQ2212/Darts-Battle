package controllers;

import btl_ltm_n3.Main;
import static btl_ltm_n3.Main.socketHandler;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
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
        Main.notifications().info("Hình ảnh và font chữ","Nền rừng: Cartoon Forest 2D Backgrounds — CraftPix.net\n"
            +"https://opengameart.org/content/cartoon-forest-2d-backgrounds\n"
            +"Giấy phép: OGA-BY 3.0 — https://static.opengameart.org/OGA-BY-3.0.txt\n"
            +"Ảnh gốc không sửa; chỉ co giãn/cắt khung khi hiển thị.\n\n"
            +"Font Lilita One — Juan Montoreano, SIL OFL 1.1 (Google Fonts).\n"
            +"Icon Crosshair Pack — Kenney.nl, CC0.\n\n"
            +"Không sử dụng ảnh tạo bằng AI. Giấy phép được đóng kèm client.");
    }

    public void handleLogout() {
        Main.notifications().confirm("Hẹn gặp lại!","Bạn muốn đăng xuất khỏi tài khoản hiện tại?","Đăng xuất",()->socketHandler.logout());
    }

    public void handleStartGame() {
        try {
            Main.setRoot("chooseOpponent");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleClickStartGameWithBot() {
        VBox body=new VBox(10,new Label("Chọn độ khó của bot"));
        final ui.Notifications.Notice[] notice=new ui.Notifications.Notice[1];
        for(enums.BotDifficulty difficulty:enums.BotDifficulty.values()){
            Button button=new Button(difficulty.label());
            button.setId("botDifficulty"+difficulty.name());button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("bot-difficulty-button");
            button.setOnAction(event->{notice[0].close();socketHandler.createBotMatch(difficulty);});
            body.getChildren().add(button);
        }
        notice[0]=Main.notifications().custom("Chơi với bot",body,null,390);
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
            Main.historyPlayerId=Main.socketHandler.loggedInUserId;Main.historyPlayerName=Main.socketHandler.loginUser;Main.historyPage=0;Main.historyOrigin="home";
            Main.setRoot("match_history");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
