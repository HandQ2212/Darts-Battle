package controllers;
import btl_ltm_n3.Main;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label connectionStatusLabel;
    @FXML private Button loginButton;
    @FXML private AnchorPane rootPane;
    @FXML public void initialize() {
        usernameField.setText(ConnectionSettings.lastUsername());
        Main.socketHandler.setConnectionListener((ready,text)->{connectionStatusLabel.setText(text);loginButton.setDisable(!ready);});
        Main.socketHandler.ensureConnected(null);
        usernameField.setOnAction(e->handleLogin());passwordField.setOnAction(e->handleLogin());
        var image=new javafx.scene.image.Image(getClass().getResource("/images/background.jpg").toExternalForm());
        rootPane.setBackground(new javafx.scene.layout.Background(new javafx.scene.layout.BackgroundImage(image,
                javafx.scene.layout.BackgroundRepeat.NO_REPEAT,javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                javafx.scene.layout.BackgroundPosition.CENTER,new javafx.scene.layout.BackgroundSize(100,100,true,true,true,false))));
    }
    @FXML public void handleLogin() {
        if(loginButton.isDisabled())return;
        Main.socketHandler.login(usernameField.getText().trim(),passwordField.getText());
    }
    @FXML public void handleRetry(){Main.socketHandler.ensureConnected(null);}
    @FXML public void goToRegister(){try{Main.setRoot("register");}catch(java.io.IOException e){Main.socketHandler.showError(e.getMessage());}}
}
