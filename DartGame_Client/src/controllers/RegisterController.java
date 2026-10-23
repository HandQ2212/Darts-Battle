package controllers;

import btl_ltm_n3.Main;
import static btl_ltm_n3.Main.socketHandler;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private AnchorPane rootPane;
    @FXML private javafx.scene.control.Label connectionStatusLabel;
    @FXML private javafx.scene.control.Button registerButton;

    @FXML
    public void initialize() {
        socketHandler.setConnectionListener((ready,text)->{connectionStatusLabel.setText(text);registerButton.setDisable(!ready);});
        socketHandler.ensureConnected(null);
    }

    public void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        socketHandler.register(username, password);
    }

    
    public void goToLogin() {
        try {
            Main.setRoot("login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.show();
    }
}
