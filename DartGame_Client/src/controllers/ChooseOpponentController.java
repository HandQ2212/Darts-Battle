package controllers;

import btl_ltm_n3.Main;
import static btl_ltm_n3.Main.socketHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import models.User;

import java.util.List;
import javafx.scene.layout.AnchorPane;

public class ChooseOpponentController {

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, Long> colId;

    @FXML
    private TableColumn<User, String> colUsername;

    @FXML
    private TableColumn<User, String> colStatus;
    @FXML private AnchorPane rootPane;
    @FXML private javafx.scene.control.Label lobbyStatusLabel;
    public void showStatus(String text) { if(lobbyStatusLabel!=null)lobbyStatusLabel.setText(text); }

    @FXML
    public void initialize() {
        var bgImage = new javafx.scene.image.Image(getClass().getResource("/images/background.jpg").toExternalForm());
        var bg = new javafx.scene.layout.BackgroundImage(
            bgImage,
            javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
            javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
            javafx.scene.layout.BackgroundPosition.CENTER,
            new javafx.scene.layout.BackgroundSize(100, 100, true, true, true, false)
        );
        rootPane.setBackground(new javafx.scene.layout.Background(bg));
       
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleLongProperty(cellData.getValue().getId()).asObject());
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().name() : "OFFLINE"));

        colStatus.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Text text = new Text(status);
                    text.setFill(status.equalsIgnoreCase("IDLE") || status.equalsIgnoreCase("ONLINE") ? Color.GREEN : Color.RED);
                    setGraphic(text);
                }
            }
        });

        showStatus(socketHandler.lobbyNotice);
        // Load lần đầu
        updateUserTable(Main.listOnlineUser);
        socketHandler.getListOnline();
    }

    // Hàm public để update bảng
    public void updateUserTable(List<User> users) {
        Platform.runLater(() -> {
            userTable.getItems().setAll(users);
        });
    }

    @FXML
    private void handleReload() {
        socketHandler.getListOnline();
        updateUserTable(Main.listOnlineUser);
    }

    @FXML
    private void handleBack() {
        try {
            Main.setRoot("home");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleConfirm() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getStatus() == enums.PlayerStatus.IDLE) {
            System.out.println("Opponent chosen: " + selected.getUsername());
            
            // Gửi lời mời Đối thủ => Server: Check + mời
            socketHandler.inviteToPlay(selected.getUsername());
        } else {
            showStatus("Hãy chọn đối thủ đang ở trạng thái IDLE.");
        }
    }
}