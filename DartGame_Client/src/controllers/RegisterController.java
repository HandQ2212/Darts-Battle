package controllers;

import btl_ltm_n3.Main;
import static btl_ltm_n3.Main.socketHandler;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private AnchorPane rootPane;
    @FXML private javafx.scene.control.Label connectionStatusLabel;
    @FXML private javafx.scene.control.Label formErrorLabel;
    @FXML private javafx.scene.control.Button registerButton;

    @FXML
    public void initialize() {
        usernameField.textProperty().addListener((o,old,value)->clearError());
        passwordField.textProperty().addListener((o,old,value)->clearError());
        socketHandler.setConnectionListener((ready,text)->{connectionStatusLabel.setText(text);registerButton.setDisable(!ready);});
        socketHandler.ensureConnected(null);
    }

    public void handleRegister() {
        clearError();
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

    private void clearError(){formErrorLabel.setVisible(false);formErrorLabel.setManaged(false);formErrorLabel.setText("");}
}
