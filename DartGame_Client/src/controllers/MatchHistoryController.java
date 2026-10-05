package controllers;

import btl_ltm_n3.Main;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import java.util.ArrayList;
import java.util.List;

public class MatchHistoryController {

    @FXML private AnchorPane rootPane;

    @FXML private TableView<MatchHistoryData> historyTable;
    @FXML private TableColumn<MatchHistoryData, String> usernameCol;
    @FXML private TableColumn<MatchHistoryData, String> opponentCol;
    @FXML private TableColumn<MatchHistoryData, String> resultCol;
    @FXML private TableColumn<MatchHistoryData, String> timeCol;

    @FXML private HBox loadingBox;

    private List<MatchHistoryData> matchList = new ArrayList<>();

    @FXML
    public void initialize() {

        // Load background
        var bgImage = new javafx.scene.image.Image(
                getClass().getResource("/images/background.jpg").toExternalForm()
        );
        var bg = new javafx.scene.layout.BackgroundImage(
                bgImage,
                javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                javafx.scene.layout.BackgroundPosition.CENTER,
                new javafx.scene.layout.BackgroundSize(100,100,true,true,true,false)
        );
        rootPane.setBackground(new javafx.scene.layout.Background(bg));

        setupColumns();

        // Đăng ký controller này vào Main
        Main.matchHistoryController = this;

        loadHistory();
    }

    private void setupColumns() {
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        usernameCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().username));

        opponentCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().opponent));

        resultCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().result));

        timeCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().time));
    }

    // Request server
    public void loadHistory() {
        showLoading(true);
        Main.socketHandler.getMatchHistory(Main.socketHandler.loginUser);
    }

    // Reload button
    @FXML
    public void onReloadClick() {
        loadHistory();
    }

    // Hàm được gọi từ socket handler
    public void updateMatchHistory(List<String> serverData) {
        matchList.clear();

        for (String row : serverData) {
            String[] parts = row.split("\\|");
            if (parts.length >= 4) {
                matchList.add(new MatchHistoryData(
                        parts[0], parts[1], parts[2], parts[3]
                ));
            }
        }

        Platform.runLater(() -> {
            historyTable.setItems(FXCollections.observableArrayList(matchList));
            showLoading(false);
        });
    }

    private void showLoading(boolean show) {
        Platform.runLater(() -> {
            loadingBox.setVisible(show);
            loadingBox.setManaged(show);
        });
    }

    @FXML
    public void handleBack() {
        try {
            Main.setRoot("home");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Data model
    public static class MatchHistoryData {
        public String username;
        public String opponent;
        public String result;
        public String time;

        public MatchHistoryData(String u, String o, String r, String t) {
            this.username = u;
            this.opponent = o;
            this.result = r;
            this.time = t;
        }
    }
}
