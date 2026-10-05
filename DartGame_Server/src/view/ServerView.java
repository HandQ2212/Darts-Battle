package view;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class ServerView {

    @FXML
    private TextArea textArea;

    public void initialize() {
        // Khởi tạo khi load FXML
        textArea.appendText("\nServer initialized...");
    }

    public void logMessage(String msg) {
        textArea.appendText("\n" + msg);
    }
}
