package controllers;

import btl_ltm_n3.Main;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class ChooseModeController {

    @FXML
    private VBox rootPane;

    @FXML
    private Button btnPlayWithUser;

    @FXML
    private Button btnPlayWithAI;

    @FXML
    private Button btnBack;

    @FXML
    private void initialize() {
        // Đặt ảnh nền
        var bgImage = new javafx.scene.image.Image(
            getClass().getResource("/images/background.jpg").toExternalForm()
        );
        var bg = new javafx.scene.layout.BackgroundImage(
            bgImage,
            javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
            javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
            javafx.scene.layout.BackgroundPosition.CENTER,
            new javafx.scene.layout.BackgroundSize(100, 100, true, true, true, false)
        );
        rootPane.setBackground(new javafx.scene.layout.Background(bg));

        // Gán sự kiện cho các nút
        btnPlayWithUser.setOnAction(this::handlePlayWithUser);
        btnPlayWithAI.setOnAction(this::handlePlayWithAI);
    }

    @FXML
    private void handlePlayWithUser(ActionEvent event) {
        try {
            // chuyển sang màn hình chọn người chơi
            Main.setRoot("choosecomponent");
        } catch (IOException ex) {
            Logger.getLogger(ChooseModeController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handlePlayWithAI(ActionEvent event) {
        try {
            // chuyển sang màn hình chơi với AI
            Main.setRoot("startgamewithbot");
        } catch (IOException ex) {
            Logger.getLogger(ChooseModeController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // quay lại trang home
            Main.setRoot("home");
        } catch (IOException ex) {
            Logger.getLogger(ChooseModeController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
