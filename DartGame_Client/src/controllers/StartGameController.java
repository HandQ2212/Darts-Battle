package controllers;
import btl_ltm_n3.Main;
import config.DartboardLayout;
import enums.*;
import protocol.*;
import protocol.dto.*;
import protocol.dto.GameMessages.*;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/** Rendering and aiming only: all scores, turns and phase come from the server. */
public class StartGameController {
    @FXML private ListView<String> chatList;
    @FXML private TextField chatInput, angleInput;
    @FXML private Pane gamePane, scoreboardPane;
    @FXML private Button startButton, rotateButton, soundButton, effectSoundButton;
    @FXML private Label instructionLabel;
    @FXML private VBox botTurnOverlay;
    @FXML private BorderPane rootPane;
    private static final double CX=225,CY=200;
    private final Group board=new Group(), marks=new Group();
    private final Text myScore=new Text(),otherScore=new Text();
    private final Text[] mine=new Text[3],theirs=new Text[3];
    private Line lineX,lineY;
    private Timeline xAnimation,yAnimation;
    private boolean xRunning,yRunning,pending,disposed,finished,sound=true,effects=true;
    private long displayedTurn=-1;
    private final java.util.List<javafx.scene.media.MediaPlayer> players=new java.util.ArrayList<>();
    private void playEffect(String name) {
        if(!effects || disposed)return;
        try {
            javafx.scene.media.MediaPlayer player=new javafx.scene.media.MediaPlayer(new javafx.scene.media.Media(getClass().getResource("/musics/"+name+".mp3").toExternalForm()));
            players.add(player);
            Runnable release=()->{players.remove(player);player.dispose();};
            player.setOnEndOfMedia(release);player.setOnError(release);player.play();
        } catch(RuntimeException e) {System.err.println("Sound unavailable: "+e.getMessage());}
    }
    @FXML public void initialize() {
        drawBoard();gamePane.getChildren().addAll(board,marks);
        lineX=new Line(0,0,0,400);lineY=new Line(0,0,450,0);
        lineX.setStroke(Color.ORANGE);lineY.setStroke(Color.ORANGE);
        lineX.setStrokeWidth(3);lineY.setStrokeWidth(3);
        gamePane.getChildren().addAll(lineX,lineY);stopAim();
        scoreboardPane.getChildren().addAll(new Text(15,25,Main.socketHandler.loginUser),new Text(15,65,Main.socketHandler.competitor));
        myScore.setX(420);myScore.setY(25);otherScore.setX(420);otherScore.setY(65);
        myScore.setFont(Font.font(20));otherScore.setFont(Font.font(20));
        scoreboardPane.getChildren().addAll(myScore,otherScore);
        for(int i=0;i<3;i++) {mine[i]=new Text(200+i*60,25,"0");theirs[i]=new Text(200+i*60,65,"0");scoreboardPane.getChildren().addAll(mine[i],theirs[i]);}
        angleInput.setOnAction(e->handleRotateBoard());
        chatInput.setOnAction(e->handleSend());
        startButton.setDisable(true);rotateButton.setDisable(true);angleInput.setDisable(true);
        render(Main.gameViewState.getSnapshot());
    }
    private void drawBoard() {
        for(int i=0;i<20;i++) {
            board.getChildren().add(section(i,0,DartboardLayout.OUTBOARD_RADIUS,i%2==0?Color.BLACK:Color.WHITE));
            board.getChildren().add(section(i,DartboardLayout.DOUBLE_INNER_RADIUS,DartboardLayout.DOUBLE_OUTER_RADIUS,i%2==0?Color.RED:Color.GREEN));
            board.getChildren().add(section(i,DartboardLayout.TRIPLE_INNER_RADIUS,DartboardLayout.TRIPLE_OUTER_RADIUS,i%2==0?Color.RED:Color.GREEN));
            double a=Math.toRadians(i*18-90);
            Text text=new Text(CX+185*Math.cos(a)-8,CY+185*Math.sin(a)+5,String.valueOf(DartboardLayout.BOARD_ORDER[i]));
            text.setFont(Font.font("Arial",FontWeight.BOLD,14));board.getChildren().add(text);
        }
        board.getChildren().addAll(new Circle(CX,CY,DartboardLayout.OUTER_BULL_RADIUS,Color.GREEN),new Circle(CX,CY,DartboardLayout.BULLSEYE_RADIUS,Color.RED));
    }
    private Polygon section(int sector,double inner,double outer,Color color) {
        Polygon p=new Polygon();double start=sector*18-99,end=start+18;
        if(inner==0)p.getPoints().addAll(CX,CY);
        for(int j=0;j<=18;j++) {double a=Math.toRadians(start+j);p.getPoints().addAll(CX+outer*Math.cos(a),CY+outer*Math.sin(a));}
        if(inner>0)for(int j=18;j>=0;j--) {double a=Math.toRadians(start+j);p.getPoints().addAll(CX+inner*Math.cos(a),CY+inner*Math.sin(a));}
        p.setFill(color);p.setStroke(Color.GRAY);p.setStrokeWidth(0.4);return p;
    }
    public void render(MatchSnapshot s) {
        if(disposed || s==null)return;
        if(displayedTurn!=s.turnId()) {
            stopAim();pending=false;marks.getChildren().clear();displayedTurn=s.turnId();
            Text[] row=Main.gameViewState.isMyTurn()?mine:theirs;for(Text t:row)t.setText("0");
        }
        board.getTransforms().setAll(new Rotate(s.rotationDegree(),CX,CY));
        boolean p1=Main.gameViewState.isPlayer1();
        myScore.setText(String.valueOf(p1?s.player1Score():s.player2Score()));otherScore.setText(String.valueOf(p1?s.player2Score():s.player1Score()));
        boolean me=Main.gameViewState.isMyTurn() && s.status()!=MatchStatus.FINISHED;
        boolean rotate=me && s.status()==MatchStatus.WAITING_ROTATION && !pending;
        rotateButton.setDisable(!rotate);angleInput.setDisable(!rotate);
        botTurnOverlay.setVisible(s.actorType()==ActorType.BOT && s.status()!=MatchStatus.FINISHED);
        if(!me || s.status()!=MatchStatus.PLAYING)stopAim();
        startButton.setDisable(!me || s.status()!=MatchStatus.PLAYING || pending || s.dartsThrown()>=3);
        if(s.status()==MatchStatus.FINISHED) {instructionLabel.setText("Trận đấu đã kết thúc.");return;}
        if(!me)instructionLabel.setText("Đang chờ "+(s.actorId()==null?"BOT":s.actorId()==s.player1Id()?s.player1Name():s.player2Name()));
        else if(s.status()==MatchStatus.WAITING_ROTATION)instructionLabel.setText("Lượt bạn: nhập góc 0–359 rồi bấm Xoay.");
        else if(pending)instructionLabel.setText("Đang chờ server xác nhận...");
        else if(s.dartsThrown()<3) {instructionLabel.setText("Phi tiêu "+(s.dartsThrown()+1)+"/3 — dừng X rồi dừng Y.");if(!xRunning&&!yRunning)startX();}
    }
    public void showThrow(ThrowResolvedResponse res) {
        if(disposed)return;pending=false;stopAim();playEffect("dart");
        marks.getChildren().add(new Circle(CX+res.getX(),CY+res.getY(),4,Color.PURPLE));
        Text[] row=java.util.Objects.equals(res.getActorId(),Main.socketHandler.loggedInUserId)?mine:theirs;
        if(res.getDartIndex()>=1&&res.getDartIndex()<=3)row[res.getDartIndex()-1].setText(res.isBust()?"BUST":String.valueOf(res.getDartScore()));
        render(res.getSnapshot());
    }
    @FXML public void handleRotateBoard() {
        MatchSnapshot s=Main.gameViewState.getSnapshot();
        if(disposed||pending||!Main.gameViewState.isMyTurn()||s.status()!=MatchStatus.WAITING_ROTATION)return;
        try {int angle=Integer.parseInt(angleInput.getText().trim());if(angle<0||angle>359)throw new NumberFormatException();
            pending=true;render(s);Main.socketHandler.submitRotation(angle);
        } catch(NumberFormatException e) {instructionLabel.setText("Nhập số nguyên từ 0 đến 359.");}
    }
    public void rotationConfirmed(MatchSnapshot s) {pending=false;render(s);}
    private void startX() {
        xRunning=true;lineX.setVisible(true);lineX.setStartX(0);lineX.setEndX(0);startButton.setText("STOP X");
        xAnimation=new Timeline(new KeyFrame(Duration.millis(25),e->{double x=(lineX.getStartX()+4)%450;lineX.setStartX(x);lineX.setEndX(x);}));
        xAnimation.setCycleCount(Animation.INDEFINITE);xAnimation.play();
    }
    @FXML public void handleGameAction() {
        if(disposed||pending||!Main.gameViewState.isMyTurn())return;
        if(xRunning) {
            xRunning=false;xAnimation.stop();yRunning=true;lineY.setVisible(true);lineY.setStartY(0);lineY.setEndY(0);startButton.setText("THROW!");
            yAnimation=new Timeline(new KeyFrame(Duration.millis(25),e->{double y=(lineY.getStartY()+4)%400;lineY.setStartY(y);lineY.setEndY(y);}));
            yAnimation.setCycleCount(Animation.INDEFINITE);yAnimation.play();
        } else if(yRunning) {
            double x=lineX.getStartX()-CX,y=lineY.getStartY()-CY;stopAim();pending=true;render(Main.gameViewState.getSnapshot());
            Main.socketHandler.submitThrowCoordinate(x,y);
        }
    }
    private void stopAim() {if(xAnimation!=null)xAnimation.stop();if(yAnimation!=null)yAnimation.stop();xRunning=false;yRunning=false;if(lineX!=null)lineX.setVisible(false);if(lineY!=null)lineY.setVisible(false);}
    public void commandFailed() {if(disposed)return;pending=false;render(Main.gameViewState.getSnapshot());}
    public void finish(MatchSnapshot s) {
        if(disposed||finished)return;finished=true;pending=false;stopAim();render(s);
        boolean won=java.util.Objects.equals(s.winnerId(),Main.socketHandler.loggedInUserId);
        playEffect(won?"win":"loose");
        String reason=switch(s.endReason()) {case "DISCONNECT"->"Mất kết nối";case "FORFEIT"->"Bỏ cuộc";case "TURN_TIMEOUT"->"Hết thời gian lượt";default->"Về 0 điểm";};
        Alert alert=new Alert(Alert.AlertType.INFORMATION,(won?"Bạn thắng! ":"Bạn thua. ")+reason,ButtonType.OK);
        alert.setHeaderText("Kết thúc trận đấu");alert.setOnHidden(e->goHome());alert.show();
    }
    @FXML public void handleBack() {
        if(finished){goHome();return;}
        Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"Rời trận sẽ bị xử thua. Tiếp tục?",ButtonType.YES,ButtonType.NO);
        alert.setOnHidden(e->{if(alert.getResult()==ButtonType.YES){pending=true;stopAim();startButton.setDisable(true);Main.socketHandler.leaveGame();}});alert.show();
    }
    private void goHome() {if(disposed)return;try{Main.setRoot("home");}catch(java.io.IOException e){Main.socketHandler.showError(e.getMessage());}}
    @FXML public void handleSend() {
        String text=chatInput.getText().trim();if(text.isEmpty()||text.length()>500||finished)return;
        Main.socketHandler.sendData(Message.of(MessageType.CHAT_MESSAGE,new ChatMessage(Main.socketHandler.roomIdPresent,text,null)));chatInput.clear();
    }
    public void addMessage(String text) {if(!disposed){chatList.getItems().add(text);if(chatList.getItems().size()>300)chatList.getItems().remove(0);chatList.scrollTo(chatList.getItems().size()-1);}}
    public void dispose() {disposed=true;stopAim();players.forEach(javafx.scene.media.MediaPlayer::dispose);players.clear();}
    @FXML private void toggleSound() {sound=!sound;soundButton.setText(sound?"🔊":"🔇");if(sound)Main.playBackgroundMusic();else Main.stopBackgroundMusic();}
    @FXML private void toggleEffectSound() {effects=!effects;effectSoundButton.setText(effects?"💥":"🔇FX");}
}
