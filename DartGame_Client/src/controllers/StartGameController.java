package controllers;

import btl_ltm_n3.Main;
import static btl_ltm_n3.Main.socketHandler;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.Group;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.transform.Rotate;

public class StartGameController implements Initializable {

    @FXML
    private ListView<String> chatList;

    @FXML
    private TextField chatInput;
    
    @FXML
    private Pane gamePane;
    
    @FXML
    private Pane scoreboardPane;
    
    @FXML
    private Button startButton;
    
    @FXML
    private Label instructionLabel;
    
    @FXML
    private TextField angleInput;

    @FXML
    private VBox botTurnOverlay;
    
    @FXML
    private Button soundButton;

    private boolean soundOn = true;

    private double boardRotation = 0; // Góc xoay hiện tại

    private Group dartboardGroup = new Group(); // <--- thêm group riêng cho dartboard

    // Game variables
    private static final double BOARD_SIZE = 400;
    private static final double CENTER_X = 225; // Center of 450px width
    private static final double CENTER_Y = BOARD_SIZE / 2;
    private static final double RADIUS = BOARD_SIZE / 2.5;
    private static final int[] POINTS = {20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5};
    
    private Line lineX, lineY;
    private Timeline lineXAnimation, lineYAnimation;
    private boolean started = true;
    private boolean inXAxis = false;
    private boolean inYAxis = false;
    
    private int playerScore = 301;
    private int opponentScore = 301;
    private int currentDart = 0; // Track current dart in turn (0, 1, 2)
    private int[] currentTurnScores = new int[3]; // Store scores for current turn
    private boolean isMyTurn = false;
    private boolean gameOver = false;
    
    private List<Circle> darts = new ArrayList<>();
    
    private Rectangle scoreBoard;
    private Text playerScoreText, opponentScoreText;
    private Text[] playerTurnTexts = new Text[3];
    private Text[] opponentTurnTexts = new Text[3];

    
    private boolean effectSoundOn = true;
    @FXML private Button effectSoundButton;
    
    @FXML private BorderPane rootPane;
     
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupGame();
        setupButtonEffects();
        
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
        
        // Tối ưu hiển thị ListView (text dài tự xuống dòng, không lag)
        chatList.setCellFactory(lv -> {
            Label label = new Label();
            label.setWrapText(true);
            label.setMaxWidth(320); // Giới hạn theo width của chat pane
            label.setStyle("-fx-font-size: 13px; -fx-padding: 5; -fx-text-fill: black;");

            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        label.setText(item);
                        setGraphic(label);
                    }
                }
            };

            // ?Cho phép cell co giãn chiều cao đúng với nội dung
            cell.setPrefWidth(0);
            cell.setWrapText(true);
            return cell;
        });

        chatList.setFixedCellSize(-1);
        gamePane.setStyle("-fx-background-color: transparent;");
        chatList.setStyle("-fx-control-inner-background: transparent; "
                + "-fx-background-color: transparent; "
                + "-fx-background-insets: 0;");
//        label.setStyle("-fx-font-size: 13px; -fx-padding: 5; -fx-text-fill: white;");
        scoreboardPane.setStyle("-fx-background-color: transparent;");
        botTurnOverlay.setStyle("-fx-background-color: transparent;");
    }

    private void setupButtonEffects() {
        startButton.setOnMouseEntered(e -> {
            startButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #5CBF60, #4CAF50); " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 16px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 15, 0, 0, 8); " +
                "-fx-cursor: hand; " +
                "-fx-opacity: 1.0; " +
                "-fx-scale-x: 1.05; " +
                "-fx-scale-y: 1.05;"
            );
        });
        startButton.setOnMouseExited(e -> {
            startButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #4CAF50, #45a049); " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 16px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5); " +
                "-fx-cursor: hand; " +
                "-fx-opacity: 0.95; " +
                "-fx-scale-x: 1.0; " +
                "-fx-scale-y: 1.0;"
            );
        });
    }

    private void setupGame() {
        gamePane.setPrefSize(450, 400);

        // Vẽ dartboard vào group
        drawDartboard();
        gamePane.getChildren().add(dartboardGroup);

        createScoreboard();

        // Create moving lines
        lineX = new Line(0, 0, 0, BOARD_SIZE);
        lineX.setStroke(Color.ORANGE);
        lineX.setStrokeWidth(3);
        lineX.setVisible(false);
        gamePane.getChildren().add(lineX);

        lineY = new Line(0, 0, 450, 0); // Updated to match pane width
        lineY.setStroke(Color.ORANGE);
        lineY.setStrokeWidth(3);
        lineY.setVisible(false);
        gamePane.getChildren().add(lineY);
        
        instructionLabel.setText("Đang chờ đến lượt của bạn ...");
        startButton.setDisable(true);
        
        if(socketHandler.checkYouAreInvited){
            startButton.setText("THROW!");
        }  
    }

    private void drawDartboard() {
        double angleStep = Math.PI * 2 / POINTS.length;

        // Clear group trước khi vẽ
        dartboardGroup.getChildren().clear();

        for (int i = 0; i < POINTS.length; i++) {
            Color singleColor = (i % 2 == 0) ? Color.BLACK : Color.WHITE;
            Color otherColor = (i % 2 == 0) ? Color.RED : Color.GREEN;

            Polygon single = createDartSection(i, angleStep, 0, RADIUS, singleColor);
            single.setUserData(POINTS[i]);
            dartboardGroup.getChildren().add(single);

            Polygon doubleArea = createDartSection(i, angleStep, RADIUS - 20, RADIUS, otherColor);
            doubleArea.setUserData(POINTS[i] * 2);
            dartboardGroup.getChildren().add(doubleArea);

            Polygon tripleArea = createDartSection(i, angleStep, RADIUS/2, RADIUS/2 + 20, otherColor);
            tripleArea.setUserData(POINTS[i] * 3);
            dartboardGroup.getChildren().add(tripleArea);

            // Add numbers
            double angle = angleStep * i - Math.PI / 2;
            double numberX = CENTER_X + (RADIUS + 30) * Math.cos(angle);
            double numberY = CENTER_Y + (RADIUS + 30) * Math.sin(angle);

            Text number = new Text(numberX - 10, numberY + 5, String.valueOf(POINTS[i]));
            number.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            number.setFill(Color.BLACK);
            dartboardGroup.getChildren().add(number);
        }

        Circle outerBull = new Circle(CENTER_X, CENTER_Y, 25, Color.GREEN);
        outerBull.setUserData(25);
        dartboardGroup.getChildren().add(outerBull);

        Circle bullsEye = new Circle(CENTER_X, CENTER_Y, 12, Color.RED);
        bullsEye.setUserData(50);
        dartboardGroup.getChildren().add(bullsEye);
    }

    private Polygon createDartSection(int section, double angleStep, double innerRadius, double outerRadius, Color color) {
        Polygon polygon = new Polygon();
        
        double startAngle = angleStep * section - angleStep/2 - Math.PI/2;
        double endAngle = angleStep * section + angleStep/2 - Math.PI/2;
        
        // Create points for the polygon
        if (innerRadius == 0) {
            // For center sections, start from center
            polygon.getPoints().addAll(new Double[]{
                CENTER_X, CENTER_Y
            });
        }
        
        // Add outer arc points
        for (double angle = startAngle; angle <= endAngle; angle += 0.1) {
            double x = CENTER_X + outerRadius * Math.cos(angle);
            double y = CENTER_Y + outerRadius * Math.sin(angle);
            polygon.getPoints().addAll(new Double[]{x, y});
        }
        
        // Add inner arc points (if not center section)
        if (innerRadius > 0) {
            for (double angle = endAngle; angle >= startAngle; angle -= 0.1) {
                double x = CENTER_X + innerRadius * Math.cos(angle);
                double y = CENTER_Y + innerRadius * Math.sin(angle);
                polygon.getPoints().addAll(new Double[]{x, y});
            }
        }
        
        polygon.setFill(color);
        polygon.setStroke(Color.BLACK);
        polygon.setStrokeWidth(1);
        
        return polygon;
    }
    
    private void nextTurn() {
        currentDart++;

        if (currentDart < 3) {
            // Vẫn còn phi tiêu → tiếp tục ném
            instructionLabel.setText("Dart " + (currentDart + 1) + "/3");
            startXAxis();
        } else {
            currentDart = 0;
            // Hết lượt → gửi kết quả cho server
            String msg = "THROW_RESULT;" + socketHandler.loginUser + ";" + socketHandler.competitor + ";" + socketHandler.roomIdPresent + ";"
                + currentTurnScores[0] + ";"
                + currentTurnScores[1] + ";"
                + currentTurnScores[2] + ";"
                + playerScore;
            socketHandler.sendData(msg);
            // Reset turn
            isMyTurn = false;
            startButton.setDisable(true);
            instructionLabel.setText("Đang chờ máy chủ ...");
        }
    }

    public void clearTurnScores() {
        currentTurnScores = new int[3];
        for (int i = 0; i < 3; i++) {
            if (isMyTurn) {
                playerTurnTexts[i].setText("0");
            } else {
                opponentTurnTexts[i].setText("0");
            }
        }
    }
    
    private void updateScoreboard() {
        playerScoreText.setText(String.valueOf(playerScore));
        opponentScoreText.setText(String.valueOf(opponentScore));
    }

    private void startXAxis() {
        inXAxis = true;
        lineX.setVisible(true);
        lineX.setStartX(0);
        lineX.setEndX(0);
        startButton.setText("STOP X");
        
        // Animate X line
        lineXAnimation = new Timeline(
            new KeyFrame(Duration.millis(50), e -> {
                double currentX = lineX.getStartX();
                currentX += 5;
                if (currentX > 450) currentX = 0; // Updated to match pane width
                lineX.setStartX(currentX);
                lineX.setEndX(currentX);
            })
        );
        lineXAnimation.setCycleCount(Timeline.INDEFINITE);
        lineXAnimation.play();
    }
    
    private void stopXAxis() {
        inXAxis = false;
        lineXAnimation.stop();
        startYAxis();
    }
    
    private void startYAxis() {
        inYAxis = true;
        lineY.setVisible(true);
        lineY.setStartY(0);
        lineY.setEndY(0);
        startButton.setText("THROW!");
        
        // Animate Y line
        lineYAnimation = new Timeline(
            new KeyFrame(Duration.millis(50), e -> {
                double currentY = lineY.getStartY();
                currentY += 5;
                if (currentY > BOARD_SIZE) currentY = 0;
                lineY.setStartY(currentY);
                lineY.setEndY(currentY);
            })
        );
        lineYAnimation.setCycleCount(Timeline.INDEFINITE);
        lineYAnimation.play();
    }
    
    private void stopYAxis() {
        inYAxis = false;
        lineYAnimation.stop();
        throwDart();
    }
    
    private void throwDart() {
        playEffect("/musics/dart.mp3");
        double dartX = lineX.getStartX();
        double dartY = lineY.getStartY();

        // Vẽ phi tiêu
        Circle dart = new Circle(dartX, dartY, 3, Color.PURPLE);
        dartboardGroup.getChildren().add(dart);
        darts.add(dart);

        // Ẩn line
        lineX.setVisible(false);
        lineY.setVisible(false);

        // Tính điểm
        int score = calculateScore(dartX, dartY);
        currentTurnScores[currentDart] = score;

        // Cập nhật UI hiển thị điểm từng lượt
        if (isMyTurn) {
            playerTurnTexts[currentDart].setText(String.valueOf(score));
        }

        // Cập nhật tạm thời điểm (nếu không bust)
        if (isMyTurn) {
            if (playerScore - score >= 0) {
                playerScore -= score;
                updateScoreboard();
            } else {
                // Bust → lượt này tính 0 điểm
                showScorePopup(dartX, dartY, 0, "BUST!");
                // Không trừ điểm, coi như ném hụt
            }
        }

        // Show popup điểm
        showScorePopup(dartX, dartY, score, score > 0 ? score + "!" : "MISS");

        // Sau khi xử lý xong 1 phi tiêu → chuyển sang phi tiêu tiếp theo
        Timeline delay = new Timeline(new KeyFrame(Duration.seconds(1), e -> nextTurn()));
        delay.play();
    }

    
    private int calculateScore(double x, double y) {
        double dx = x - CENTER_X;
        double dy = y - CENTER_Y;

        // Ngược xoay tọa độ về hệ tọa độ gốc của bàn
        double theta = Math.toRadians(-boardRotation);
        double rotatedX = dx * Math.cos(theta) - dy * Math.sin(theta);
        double rotatedY = dx * Math.sin(theta) + dy * Math.cos(theta);

        // Tính khoảng cách từ tâm
        double distance = Math.sqrt(rotatedX*rotatedX + rotatedY*rotatedY);

        // Kiểm tra bullseye và outer bull (tâm, 25d và 0đ)
        if (distance <= 12) return 50;
        if (distance <= 25) return 25;
        if (distance > RADIUS) return 0;

        // Tính góc (atan2 trả về [-π, π])
        double angle = Math.atan2(rotatedY, rotatedX);
        
        // Chuyển về [0, 2π] và điều chỉnh để khớp với cách vẽ section
        // Trong createDartSection, section 0 bắt đầu từ góc -Math.PI/2 (12 giờ)
        angle = angle + Math.PI/2;  // Xoay để 0° ở vị trí 12 giờ
        if (angle < 0) angle += 2 * Math.PI;  // Đảm bảo angle >= 0
        
        
        // Tính góc của mỗi "miếng điểm số"
        double sectionAngle = 2 * Math.PI / POINTS.length;
        // Điều chỉnh để góc nằm giữa section (vì section được vẽ từ -angleStep/2 đến +angleStep/2)
        angle = (angle + sectionAngle/2) % (2 * Math.PI);
        
        int section = (int)(angle / sectionAngle);
        
        // Đảm bảo section trong phạm vi hợp lệ
        if (section < 0) section = 0;
        if (section >= POINTS.length) section = POINTS.length - 1;
        
        // Lấy ra điểm 
        int baseScore = POINTS[section];

        // Kiểm tra vùng double (vòng ngoài)
        if (distance >= RADIUS - 20 && distance <= RADIUS) {
            return baseScore * 2;
        }
        // Kiểm tra vùng triple (vòng giữa)
        else if (distance >= RADIUS/2 && distance <= RADIUS/2 + 20) {
            return baseScore * 3;
        }
        // Vùng single
        else {
            return baseScore;
        }
    }
   
    private void showScorePopup(double x, double y, int score, String message) {
        Text popup = new Text(x, y - 20, message);
        popup.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        popup.setFill(score > 0 ? Color.YELLOW : Color.RED);
        gamePane.getChildren().add(popup);
        
        // Remove popup after delay
        Timeline removePopup = new Timeline(new KeyFrame(Duration.seconds(0.8), e -> 
            gamePane.getChildren().remove(popup)));
        removePopup.play();
    }
    
    private void endGame(boolean playerWon) {
        started = false;
        gameOver = true;
        startButton.setText("START GAME");
        startButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #4CAF50, #45a049); " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 16px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5); " +
            "-fx-cursor: hand; " +
            "-fx-opacity: 0.95;"
        );
        
        String winner = playerWon ? 
                (socketHandler.loginUser + " là người chiến thắng !!!")
                : (socketHandler.competitor + " là người chiến thắng !!!");
        instructionLabel.setText(winner + " - Click START GAME to play again");
        
//        winnerText.setText(playerWon ? "Player là người chiến thắng !!!" : "Computer là người chiến thắng !!!");
        
        // Clear darts
        for (Circle dart : darts) {
            gamePane.getChildren().remove(dart);
        }
        darts.clear();
    }

    public void handleGameAction() {
        if (inXAxis) {
            stopXAxis();
        } else if (inYAxis) {
            stopYAxis();
        }
    }

    private void createScoreboard() {
        double boardWidth = 530; // Reduced from 980 to fit in 630px container
        double boardHeight = 80;
        double padding = 10;
        
        scoreBoard = new Rectangle(padding, padding, boardWidth, boardHeight);
        scoreBoard.setFill(Color.LIGHTGRAY);
        scoreBoard.setStroke(Color.BLACK);
        scoreBoard.setStrokeWidth(2);
        scoreboardPane.getChildren().add(scoreBoard);
        
        // Columns: Tên người chơi/Lượt (150px), 1 (60px), 2 (60px), 3 (60px), Điểm còn lại (120px), Người chiến thắng (160px)
        Line vLine1 = new Line(padding + 150, padding, padding + 150, padding + boardHeight);
        Line vLine2 = new Line(padding + 210, padding, padding + 210, padding + boardHeight);
        Line vLine3 = new Line(padding + 270, padding, padding + 270, padding + boardHeight);
        Line vLine4 = new Line(padding + 330, padding, padding + 330, padding + boardHeight);
        
        Line headerSeperator = new Line(padding, padding + 25, padding + boardWidth, padding + 25); 
        headerSeperator.setStroke(Color.BLACK);
        headerSeperator.setStrokeWidth(1.5);
        
        // Horizontal line to separate Player1 and Computer rows
        Line hLine = new Line(padding, padding + boardHeight/2 + 10, padding + boardWidth, padding + boardHeight/2 + 10); 
        
        vLine1.setStroke(Color.BLACK);
        vLine2.setStroke(Color.BLACK);
        vLine3.setStroke(Color.BLACK);
        vLine4.setStroke(Color.BLACK);
        hLine.setStroke(Color.BLACK);
        
        scoreboardPane.getChildren().addAll(vLine1, vLine2, vLine3, vLine4,headerSeperator ,hLine);
        
        Text headerName = new Text(padding + 30, padding + 15, "Tên người chơi/Lượt");
        Text header1 = new Text(padding + 180, padding + 15, "1");
        Text header2 = new Text(padding + 240, padding + 15, "2");
        Text header3 = new Text(padding + 300, padding + 15, "3");
        Text headerScore = new Text(padding + 390, padding + 15, "Điểm còn lại");
        
        headerName.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        header1.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        header2.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        header3.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        headerScore.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        
        scoreboardPane.getChildren().addAll(headerName, header1, header2, header3, headerScore);
        
        // Player rows
        Text player1Name = new Text(padding + 30, padding + 40, socketHandler.loginUser);
        Text competitorName = new Text(padding + 30, padding + 70, socketHandler.competitor);
        
        player1Name.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        competitorName.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        scoreboardPane.getChildren().addAll(player1Name, competitorName);
        
        playerScoreText = new Text(padding + 410, padding + 40, "301");
        playerScoreText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        playerScoreText.setFill(Color.BLACK);
        scoreboardPane.getChildren().add(playerScoreText);
        
        opponentScoreText = new Text(padding + 410, padding + 70, "301");
        opponentScoreText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        opponentScoreText.setFill(Color.BLACK);
        scoreboardPane.getChildren().add(opponentScoreText);
        
        for (int i = 0; i < 3; i++) {
            playerTurnTexts[i] = new Text(padding + 180 + (i * 60), padding + 40, "0");
            playerTurnTexts[i].setFont(Font.font("Arial", FontWeight.BOLD, 12));
            playerTurnTexts[i].setFill(Color.BLACK);
            scoreboardPane.getChildren().add(playerTurnTexts[i]);
            
            opponentTurnTexts[i] = new Text(padding + 180 + (i * 60), padding + 70, "0");
            opponentTurnTexts[i].setFont(Font.font("Arial", FontWeight.BOLD, 12));
            opponentTurnTexts[i].setFill(Color.BLACK);
            scoreboardPane.getChildren().add(opponentTurnTexts[i]);
        }
    }
    public void handleBack() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận thoát game");
        alert.setHeaderText("Nếu thoát game, bạn sẽ bị xử thua ván này. Bạn có chắc chắn muốn thoát?");
        alert.setContentText("Hãy chọn hành động của bạn.");

        ButtonType buttonXacNhan = new ButtonType("Xác nhận");
        ButtonType buttonHuy = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonXacNhan, buttonHuy);

        alert.showAndWait().ifPresent(response -> {
            if (response == buttonXacNhan) {
                try {
                    socketHandler.leaveGame();
                    Main.setRoot("home"); // Quay lại màn hình home
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } 
            // Nếu bấm Hủy thì không làm gì
        });
    }

    public void addMessage(String message) {
        chatList.getItems().add(message);

        // Tự động scroll xuống cuối khi có tin nhắn mới
        chatList.scrollTo(chatList.getItems().size() - 1);
    }
    public void handleSend() {
        String msg = chatInput.getText().trim();
        if (!msg.isEmpty()) {
            chatList.getItems().add("Bạn: " + msg);
            chatList.scrollTo(chatList.getItems().size() - 1);
            chatInput.clear();
            
            String data = "CHAT_MESSAGE" + ";" + socketHandler.loginUser + ";" + socketHandler.competitor + ";" + socketHandler.roomIdPresent + ";" + msg;
            // send data
            socketHandler.sendData(data);
            
            // Tự động tạo tin nhắn random từ đối thủ sau khi bạn gửi
//            simulateOpponentReply();
        }
    }

    public void showWinnerDialog(String leaver) {
        playEffect("/musics/win.mp3");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Kết thúc trận đấu");
        alert.setHeaderText(null);
        alert.setContentText("Người chơi " + leaver + " đã rời khỏi phòng.\nBạn là người chiến thắng!");

        ButtonType leaveBtn = new ButtonType("Rời khỏi phòng", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(leaveBtn);

        alert.showAndWait().ifPresent(response -> {
            if (response == leaveBtn) {
                try {
                    // Quay về màn home
                    Main.setRoot("home");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public void showWinnerDialogEndGame(String winner) {
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Kết thúc trận đấu");
        alert.setHeaderText(null);
        if(winner.equals(socketHandler.loginUser)){
            playEffect("/musics/win.mp3");
            alert.setContentText("Trận đấu kết thúc, Bạn là người chiến thắng");
        }
        else{
            playEffect("/musics/loose.mp3");
            alert.setContentText("Trận đấu kết thúc, bạn là người thua cuộc");
        }

        ButtonType leaveBtn = new ButtonType("Rời khỏi phòng", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(leaveBtn);

        alert.showAndWait().ifPresent(response -> {
            if (response == leaveBtn) {
                try {
                    // Quay về màn home
                    Main.setRoot("home");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public void handleRotateBoard() {
        if (gameOver) return;

        try {
            double angle = Double.parseDouble(angleInput.getText().trim());
            boardRotation += angle;
            applyBoardRotation(boardRotation); // dùng transform với pivot cố định
            instructionLabel.setText("Bàn đã xoay " + angle + "°");
            angleInput.clear();
            
            String msg = "ROTATE_RESULT;" + socketHandler.loginUser + ";" + socketHandler.competitor +";" +socketHandler.roomIdPresent + ";" + angle;
            socketHandler.sendData(msg);
            
            angleInput.setDisable(true);   // khóa lại
            startButton.setDisable(false); // mở lại nút ném nếu là lượt mình

            // Với 2 người chơi: chỉ cần check nếu là lượt mình
            if (!isMyTurn) {
                instructionLabel.setText("Đang chờ đối thủ xoay bàn và ném...");
                startButton.setDisable(true); // không cho ném
            }
        } catch (NumberFormatException e) {
            instructionLabel.setText("Nhập số hợp lệ!");
        }
    }
    
    public void setTurn(String nextPlayer){
        if(nextPlayer.equals(socketHandler.loginUser)){
            isMyTurn = true;
            clearTurnScores();
            instructionLabel.setText("Đến lượt bạn ném");
            startButton.setDisable(false);
            angleInput.setDisable(false);
//            inXAxis = true;
            startXAxis();
        } else{
            isMyTurn = false;
            instructionLabel.setText("Đang chờ đối thủ");
            startButton.setDisable(true);
            angleInput.setDisable(true);
        }
    }
    
    public void setTurnRotate(String nextPlayer){
        instructionLabel.setText("Nhập góc xoay bàn rồi ấn Enter để tiếp tục lượt máy!");
        angleInput.setDisable(false); // cho nhập
        startButton.setDisable(true); // tạm khóa nút ném
    }
    public void updateCompetitorStatus(String competitorName,String roomId,String aigle, String score1,String score2,String score3, String scoreRemaining){
        currentTurnScores[0] = Integer.parseInt(score1);
        opponentTurnTexts[0].setText(score1);
        
        currentTurnScores[1] = Integer.parseInt(score2);
        opponentTurnTexts[1].setText(score2);
        
        currentTurnScores[2] = Integer.parseInt(score3);
        opponentTurnTexts[2].setText(score3);
        
        opponentScore = Integer.parseInt(scoreRemaining); 
        opponentScoreText.setText(String.valueOf(opponentScore));
    
        boardRotation += Double.parseDouble(aigle);
        applyBoardRotation(boardRotation);
    }


    // CHAT: 
    private void simulateOpponentReply() {
        // Danh sách câu trả lời ngẫu nhiên
        String[] replies = {
            "Haha, ném trúng chưa đó?",
            "Tới lượt tôi nè!",
            "Wow, bạn giỏi quá!",
            "Đừng xoay bàn nhiều quá nhé 😆",
            "Good luck!",
            "Ồ, điểm đó cao đấy!",
            "Lần này tôi thắng chắc rồi!",
            "Ném lệch rồi kìa 😂"
        };

        // Random 1 câu
        int randomIndex = (int) (Math.random() * replies.length);
        String reply = replies[randomIndex];

        // Hiển thị sau 1–2 giây để giống người thật
        Timeline delay = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> {
            chatList.getItems().add(socketHandler.competitor + ": " + reply);
        }));
        delay.play();
    }
    private void applyBoardRotation(double degrees) {
    // Remove previous Rotate transforms on the group (nếu có)
        dartboardGroup.getTransforms().removeIf(t -> t instanceof Rotate);
        // Add a new Rotate transform with explicit pivot at CENTER_X, CENTER_Y
        dartboardGroup.getTransforms().add(new Rotate(degrees, CENTER_X, CENTER_Y));
    }
    
    

    @FXML
    private void toggleSound() {
        soundOn = !soundOn;

        if (soundOn) {
            soundButton.setText("🔊");
            Main.playBackgroundMusic(); // bật lại nhạc (nếu đang tắt)
        } else {
            soundButton.setText("🔇");
            Main.stopBackgroundMusic(); // tắt hẳn nhạc
        }
    }
    
    @FXML
    private void toggleEffectSound() {
        effectSoundOn = !effectSoundOn;
        effectSoundButton.setText(effectSoundOn ? "💥" : "🔇FX");
    }
    
    private void playEffect(String path) {
        if (!effectSoundOn) return; // nếu tắt hiệu ứng thì không phát
        try {
            Media sound = new Media(getClass().getResource(path).toExternalForm());
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();
        } catch (Exception e) {
            System.err.println("Không thể phát âm thanh: " + path);
        }
    }
}
