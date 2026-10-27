package controllers;
import btl_ltm_n3.Main;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label connectionStatusLabel;
    @FXML private Label formErrorLabel;
    @FXML private Button loginButton;
    @FXML private AnchorPane rootPane;
    @FXML public void initialize() {
        usernameField.setText(ConnectionSettings.lastUsername());
        usernameField.textProperty().addListener((o,old,value)->clearError());
        passwordField.textProperty().addListener((o,old,value)->clearError());
        Main.socketHandler.setConnectionListener((ready,text)->{connectionStatusLabel.setText(text);loginButton.setDisable(!ready);});
        Main.socketHandler.ensureConnected(null);
        usernameField.setOnAction(e->handleLogin());passwordField.setOnAction(e->handleLogin());
    }
    @FXML public void handleLogin() {
        if(loginButton.isDisabled())return;
        clearError();
        Main.socketHandler.login(usernameField.getText().trim(),passwordField.getText());
    }
    private void clearError(){formErrorLabel.setVisible(false);formErrorLabel.setManaged(false);formErrorLabel.setText("");}
    @FXML public void handleRetry(){Main.socketHandler.ensureConnected(null);}
    @FXML public void goToRegister(){try{Main.setRoot("register");}catch(java.io.IOException e){Main.socketHandler.showError(e.getMessage());}}
}
