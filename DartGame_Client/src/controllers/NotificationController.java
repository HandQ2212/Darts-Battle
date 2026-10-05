package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class NotificationController {

    @FXML
    private Label lblMessage;

    private Runnable onAccept;
    private Runnable onDecline;

    public void setMessage(String msg) {
        lblMessage.setText(msg);
    }

    public void setOnAccept(Runnable action) {
        this.onAccept = action;
    }

    public void setOnDecline(Runnable action) {
        this.onDecline = action;
    }

    @FXML
    private void handleAccept() {
        if (onAccept != null) onAccept.run();
        closeDialog();
    }

    @FXML
    private void handleDecline() {
        if (onDecline != null) onDecline.run();
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) lblMessage.getScene().getWindow();
        stage.close();
    }
}
