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
import javafx.scene.image.Image;
import javafx.util.Duration;

/** Rendering and aiming only: all scores, turns and phase come from the server. */
public class StartGameController {
    @FXML private ListView<String> chatList;
    @FXML private TextField chatInput;
    @FXML private Pane gamePane;
    @FXML private Button soundButton, effectSoundButton;
    @FXML private Label aimStatusLabel;
    @FXML private VBox botTurnOverlay;
    @FXML private BorderPane rootPane;
    @FXML private HBox gameMain,mediaControls,chatComposer;
    @FXML private VBox leftColumn,rightColumn,myScoreSlot,otherScoreSlot;
    @FXML private StackPane localVideoFrame,remoteVideoFrame;
    @FXML private StackPane boardContainer;
    @FXML private VBox resultPanel;
    @FXML private Label resultTitle,resultScore,rematchStatus;
    @FXML private Button rematchButton,exitSessionButton;
    @FXML private BorderPane chatPanel;
    @FXML private Label mediaStatusLabel;
    @FXML private Button microphoneButton,cameraButton,deviceSettingsButton;
    private final ComboBox<String> microphoneChoice=new ComboBox<>(),speakerChoice=new ComboBox<>(),cameraChoice=new ComboBox<>();
    @FXML private javafx.scene.image.ImageView localVideoView,remoteVideoView;
    @FXML private Label localVideoPlaceholder,remoteVideoPlaceholder;
    private static final double CX=225,CY=200;
    private final Group board=new Group(), marks=new Group();
    private ui.DartThrowEffects dartEffects;
    private ui.BoardRotationEffect rotationEffect;
    private javafx.scene.media.AudioClip dartImpact;
    private final Text myScore=new Text(),otherScore=new Text();
    private ui.ScoreEffect myScoreEffect,otherScoreEffect;
    private VBox myCard, otherCard;
    @FXML private Pane turnMotionLayer;
    private Label myTurnTag,otherTurnTag;
    private ui.TurnMotion turnMotion;
    private static final javafx.css.PseudoClass ACTIVE=javafx.css.PseudoClass.getPseudoClass("active");
    private static final javafx.css.PseudoClass PENDING=javafx.css.PseudoClass.getPseudoClass("pending");
    private Line lineX,lineY;
    private Timeline xAnimation,yAnimation;
    private boolean xRunning,yRunning,pending,disposed,finished,sound=true,effects=true;
    private ui.MediaCallSession mediaCall;
    private boolean pvpMedia,callJoined,microphoneEnabled,cameraEnabled,peerInCall;
    private boolean microphoneRequested,cameraRequested,mediaStarted,historyOpen;
    private boolean lastTurnMine;
    private long animatedTurn=-1;
    private final PauseTransition remoteVideoExpiry=new PauseTransition(Duration.seconds(2));
    private ui.Notifications.Notice historyNotice;
    private ui.Notifications.Notice settingsNotice;
    private VBox historyRows,deviceSettingsPanel;
    private String activeCallId,previousCallId;
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
        rotationEffect=new ui.BoardRotationEffect(board,CX,CY);
        dartEffects=new ui.DartThrowEffects(marks,this::playDartImpact);
        try {dartImpact=new javafx.scene.media.AudioClip(getClass().getResource("/musics/dart.mp3").toExternalForm());dartImpact.setVolume(0.8);}
        catch(RuntimeException e) {System.err.println("Impact audio unavailable: "+e.getMessage());}
        lineX=new Line(0,0,0,400);lineY=new Line(0,0,450,0);
        lineX.setStroke(Color.web("#e76e12"));lineY.setStroke(Color.web("#e76e12"));
        lineX.setStrokeWidth(2);lineY.setStrokeWidth(2);
        lineX.setMouseTransparent(true);lineY.setMouseTransparent(true);
        gamePane.getChildren().addAll(lineX,lineY);marks.toFront();stopAim();
        gamePane.setPickOnBounds(true);
        gamePane.setOnMouseClicked(event->{
            if(event.getButton()==javafx.scene.input.MouseButton.PRIMARY) {
                handleGameAction();event.consume();
            }
        });
        leftColumn.prefWidthProperty().bind(javafx.beans.binding.Bindings.min(280,javafx.beans.binding.Bindings.max(220,gameMain.widthProperty().subtract(32).multiply(.24))));
        rightColumn.prefWidthProperty().bind(leftColumn.prefWidthProperty());
        bindVideo(localVideoFrame,localVideoView);bindVideo(remoteVideoFrame,remoteVideoView);
        myCard=playerCard(Main.socketHandler.loginUser,"BẠN",myScore);
        otherCard=playerCard(Main.socketHandler.competitor,"ĐỐI THỦ",otherScore);
        otherCard.getStyleClass().add("opponent-card");
        myScoreSlot.getChildren().add(myCard);otherScoreSlot.getChildren().add(otherCard);
        turnMotion=new ui.TurnMotion(myCard,myTurnTag,otherCard,otherTurnTag,turnMotionLayer);
        remoteVideoExpiry.setOnFinished(event->{if(!disposed){remoteVideoView.setImage(null);remoteVideoPlaceholder.setVisible(true);}});
        chatList.setCellFactory(list->new ListCell<>() {
            private final Label message=new Label();
            {message.setWrapText(true);message.maxWidthProperty().bind(list.widthProperty().subtract(36));}
            @Override protected void updateItem(String item,boolean empty) {
                super.updateItem(item,empty);setText(null);message.setText(empty?null:item);setGraphic(empty?null:message);
            }
        });
        chatInput.setOnAction(e->handleSend());
        mediaCall=new ui.MediaCallSession(Main.socketHandler::sendData,
                text->javafx.application.Platform.runLater(()->{if(!disposed)mediaStatusLabel.setText(text);}),
                text->javafx.application.Platform.runLater(()->{if(!disposed){mediaStatusLabel.setText(text);Main.notifications().toast("Cuộc gọi",text,"warning");}}),
                text->javafx.application.Platform.runLater(()->{if(!disposed)mediaRequestFailed(text);}),
                enabled->javafx.application.Platform.runLater(()->{if(!disposed){microphoneEnabled=enabled;if(enabled||peerInCall)microphoneRequested=enabled;updateMediaButtons();}}),
                enabled->javafx.application.Platform.runLater(()->{if(!disposed){cameraEnabled=enabled;if(enabled||peerInCall)cameraRequested=enabled;updateMediaButtons();localVideoPlaceholder.setVisible(!enabled);}}),
                image->javafx.application.Platform.runLater(()->{if(!disposed){localVideoView.setImage(image);localVideoPlaceholder.setVisible(image==null);}}),
                image->javafx.application.Platform.runLater(()->{if(!disposed){remoteVideoView.setImage(image);remoteVideoPlaceholder.setVisible(image==null);if(image==null)remoteVideoExpiry.stop();else remoteVideoExpiry.playFromStart();}}),
                devices->javafx.application.Platform.runLater(()->{
                    if(disposed)return;
                    updateChoices(microphoneChoice,devices.microphones());updateChoices(speakerChoice,devices.speakers());updateChoices(cameraChoice,devices.cameras());
                }));
        microphoneChoice.getItems().setAll("Mặc định");speakerChoice.getItems().setAll("Mặc định");cameraChoice.getItems().setAll("Mặc định");
        microphoneChoice.getSelectionModel().selectFirst();speakerChoice.getSelectionModel().selectFirst();cameraChoice.getSelectionModel().selectFirst();
        microphoneChoice.setOnAction(e->{if(microphoneRequested&&peerInCall)mediaCall.setMicrophone(true,microphoneChoice.getSelectionModel().getSelectedIndex()-1);});
        speakerChoice.setOnAction(e->mediaCall.setSpeaker(speakerChoice.getSelectionModel().getSelectedIndex()-1));
        cameraChoice.setOnAction(e->{if(cameraRequested&&peerInCall){int index=cameraChoice.getSelectionModel().getSelectedIndex()-1;if(cameraEnabled)mediaCall.selectCamera(index);else mediaCall.setCamera(true,index);}});
        updateMediaAvailability();
        updateMediaButtons();deviceSettingsButton.setGraphic(deviceIcon("settings",true));deviceSettingsButton.setAccessibleText("Cài đặt thiết bị");deviceSettingsButton.setTooltip(new Tooltip("Cài đặt thiết bị"));
        aimStatusLabel.setDisable(true);
        render(Main.gameViewState.getSnapshot());
        javafx.application.Platform.runLater(()->{if(!disposed&&pvpMedia&&!mediaStarted){mediaStarted=true;joinCall();}});
    }
    private void bindVideo(StackPane frame,javafx.scene.image.ImageView view){
        frame.prefHeightProperty().bind(frame.widthProperty().multiply(9.0/16));
        frame.minHeightProperty().bind(frame.prefHeightProperty());frame.maxHeightProperty().bind(frame.prefHeightProperty());
        view.fitWidthProperty().bind(frame.widthProperty().subtract(8));view.fitHeightProperty().bind(frame.heightProperty().subtract(8));
    }
    private void updateChoices(ComboBox<String> choice,java.util.List<String> names){
        String selected=choice.getValue();choice.getItems().setAll(java.util.stream.Stream.concat(java.util.stream.Stream.of("Mặc định"),names.stream()).toList());
        if(selected!=null&&choice.getItems().contains(selected))choice.getSelectionModel().select(selected);else choice.getSelectionModel().selectFirst();
    }
    private VBox playerCard(String name,String caption,Text score) {
        Label player=new Label(name==null?caption:name);player.getStyleClass().add("player-name");
        player.setMaxWidth(Double.MAX_VALUE);player.setMinWidth(0);player.setWrapText(true);
        score.getStyleClass().add("score-value");
        Label remaining=new Label(caption+" / CÒN LẠI");remaining.getStyleClass().add("score-caption");
        StackPane scoreRow=new StackPane();scoreRow.setMinWidth(0);scoreRow.setMaxWidth(Double.MAX_VALUE);
        ui.ScoreEffect effect=new ui.ScoreEffect(score,scoreRow);
        if(score==myScore)myScoreEffect=effect;else otherScoreEffect=effect;
        Label tag=new Label("Đến lượt");tag.setId(score==myScore?"myTurnTag":"otherTurnTag");tag.getStyleClass().add("turn-indicator");
        tag.setVisible(false);tag.setMouseTransparent(true);
        if(score==myScore)myTurnTag=tag;else otherTurnTag=tag;
        HBox heading=new HBox(5,player,tag);heading.setAlignment(javafx.geometry.Pos.CENTER_LEFT);HBox.setHgrow(player,Priority.ALWAYS);
        VBox card=new VBox(4,heading,scoreRow,remaining);card.getStyleClass().add("score-card");
        card.setMinWidth(0);card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }
    private void drawBoard() {
        Circle rim=new Circle(CX,CY,198,new javafx.scene.paint.RadialGradient(0,0,0.4,0.35,0.7,true,javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0,Color.web("#e5ad50")),new javafx.scene.paint.Stop(1,Color.web("#bb7a34"))));
        rim.setStroke(new javafx.scene.paint.LinearGradient(0,0,1,1,true,javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0,Color.web("#ffe5a0")),new javafx.scene.paint.Stop(0.5,Color.web("#c17b31")),new javafx.scene.paint.Stop(1,Color.web("#9d5d2b"))));
        rim.setStrokeWidth(3);board.getChildren().add(rim);
        for(int i=0;i<20;i++) {
            board.getChildren().add(section(i,0,DartboardLayout.OUTBOARD_RADIUS,i%2==0?Color.web("#ffe29b"):Color.web("#fff2be")));
            board.getChildren().add(section(i,DartboardLayout.DOUBLE_INNER_RADIUS,DartboardLayout.DOUBLE_OUTER_RADIUS,i%2==0?Color.web("#ff9956"):Color.web("#70b85b")));
            board.getChildren().add(section(i,DartboardLayout.TRIPLE_INNER_RADIUS,DartboardLayout.TRIPLE_OUTER_RADIUS,i%2==0?Color.web("#ff9956"):Color.web("#70b85b")));
            double a=Math.toRadians(i*18-90);
            Text text=new Text(String.valueOf(DartboardLayout.BOARD_ORDER[i]));text.getStyleClass().add("board-number");
            text.setFont(Font.font("Lilita One",17));text.setFill(Color.web("#75451e"));
            text.setX(CX+181*Math.cos(a)-text.getLayoutBounds().getWidth()/2);text.setY(CY+181*Math.sin(a)+5);board.getChildren().add(text);
        }
        board.getChildren().addAll(new Circle(CX,CY,DartboardLayout.OUTER_BULL_RADIUS,Color.web("#70b85b")),new Circle(CX,CY,DartboardLayout.BULLSEYE_RADIUS,Color.web("#ff9956")));
        javafx.scene.canvas.Canvas fibers=new javafx.scene.canvas.Canvas(320,320);
        fibers.setLayoutX(CX-160);fibers.setLayoutY(CY-160);fibers.setClip(new Circle(160,160,160));
        javafx.scene.canvas.GraphicsContext ink=fibers.getGraphicsContext2D();
        java.util.Random random=new java.util.Random(301);
        ink.setStroke(Color.color(0.85,0.94,1,0.09));ink.setLineWidth(0.6);
        for(int i=0;i<1400;i++){double x=random.nextDouble()*320,y=random.nextDouble()*320;ink.strokeLine(x,y,x+1,y+2+random.nextDouble()*3);}
        board.getChildren().add(fibers);
    }
    private Polygon section(int sector,double inner,double outer,Color color) {
        Polygon p=new Polygon();double start=sector*18-99,end=start+18;
        if(inner==0)p.getPoints().addAll(CX,CY);
        for(int j=0;j<=18;j++) {double a=Math.toRadians(start+j);p.getPoints().addAll(CX+outer*Math.cos(a),CY+outer*Math.sin(a));}
        if(inner>0)for(int j=18;j>=0;j--) {double a=Math.toRadians(start+j);p.getPoints().addAll(CX+inner*Math.cos(a),CY+inner*Math.sin(a));}
        p.setFill(color);p.setStroke(Color.web("#c69f59"));p.setStrokeWidth(0.4);return p;
    }
    public void render(MatchSnapshot s) {
        if(disposed || s==null)return;
        updateMediaAvailability();
        boolean turnChanged=displayedTurn!=s.turnId();
        if(turnChanged) {
            rotationEffect.cancel();
            stopAim();pending=false;dartEffects.beginTurn();displayedTurn=s.turnId();
            if(s.status()==MatchStatus.WAITING_ROTATION)rotationEffect.play(s.rotationDegree(),()->{
                if(!disposed && displayedTurn==s.turnId())render(Main.gameViewState.getSnapshot());
            });
        }
        if(s.status()==MatchStatus.FINISHED)rotationEffect.cancel();
        rotationEffect.setAngle(s.rotationDegree());
        boolean rotating=rotationEffect.isRunning() || s.status()==MatchStatus.WAITING_ROTATION;
        boolean p1=Main.gameViewState.isPlayer1();
        myScoreEffect.sync(p1?s.player1Score():s.player2Score());otherScoreEffect.sync(p1?s.player2Score():s.player1Score());
        boolean me=Main.gameViewState.isMyTurn() && s.status()!=MatchStatus.FINISHED;
        boolean active=s.status()!=MatchStatus.FINISHED;
        myCard.pseudoClassStateChanged(ACTIVE,me);otherCard.pseudoClassStateChanged(ACTIVE,active&&!me);
        if(active&&(turnChanged||animatedTurn!=s.turnId()||lastTurnMine!=me))animateTurn(me,s.turnId());
        if(!active)turnMotion.clear();
        botTurnOverlay.setVisible(!rotating && s.actorType()==ActorType.BOT && s.status()!=MatchStatus.FINISHED);
        if(!me || rotating || s.status()!=MatchStatus.PLAYING||historyOpen)stopAim();
        aimStatusLabel.setDisable(historyOpen||!me || rotating || s.status()!=MatchStatus.PLAYING || pending || s.dartsThrown()>=3);
        gamePane.setCursor(aimStatusLabel.isDisabled()?javafx.scene.Cursor.DEFAULT:javafx.scene.Cursor.CROSSHAIR);
        if(!active)aimStatusLabel.setText("Trận đấu đã kết thúc.");
        else if(rotating)aimStatusLabel.setText("Bảng đang xoay — vui lòng chờ.");
        else if(!me)aimStatusLabel.setText("Đang chờ đối thủ ném.");
        else if(pending)aimStatusLabel.setText("Đang chờ server xác nhận...");
        else if(historyOpen)aimStatusLabel.setText("Đang xem lịch sử lượt.");
        else if(s.dartsThrown()<3&&!xRunning&&!yRunning)startX();
    }
    public void showThrow(ThrowResolvedResponse res) {
        if(disposed)return;pending=false;stopAim();
        boolean own=java.util.Objects.equals(res.getActorId(),Main.socketHandler.loggedInUserId);
        (own?myScoreEffect:otherScoreEffect).play(res.getRemainingScoreAfter(),res.getDartScore(),res.isBust());
        dartEffects.throwDart(CX+res.getX(),CY+res.getY(),own,res.getDartScore(),res.isBust());
        if(historyOpen)refreshHistory();
        render(res.getSnapshot());
    }
    public void rotationConfirmed(MatchSnapshot s) {pending=false;render(s);}
    private void startX() {
        xRunning=true;lineX.setVisible(true);lineX.setStartX(0);lineX.setEndX(0);aimStatusLabel.setText("Click để chốt X");
        xAnimation=new Timeline(new KeyFrame(Duration.millis(25),e->{double x=(lineX.getStartX()+4)%450;lineX.setStartX(x);lineX.setEndX(x);}));
        xAnimation.setCycleCount(Animation.INDEFINITE);xAnimation.play();
    }
    @FXML public void handleGameAction() {
        if(disposed||historyOpen||pending||rotationEffect.isRunning()||!Main.gameViewState.isMyTurn()
                || Main.gameViewState.getSnapshot().status()!=MatchStatus.PLAYING)return;
        if(xRunning) {
            xRunning=false;xAnimation.stop();yRunning=true;lineY.setVisible(true);lineY.setStartY(0);lineY.setEndY(0);aimStatusLabel.setText("Click để ném");
            yAnimation=new Timeline(new KeyFrame(Duration.millis(25),e->{double y=(lineY.getStartY()+4)%400;lineY.setStartY(y);lineY.setEndY(y);}));
            yAnimation.setCycleCount(Animation.INDEFINITE);yAnimation.play();
        } else if(yRunning) {
            double x=lineX.getStartX()-CX,y=lineY.getStartY()-CY;stopAim();pending=true;render(Main.gameViewState.getSnapshot());
            Main.socketHandler.submitThrowCoordinate(x,y);
        }
    }
    private void stopAim() {if(xAnimation!=null)xAnimation.stop();if(yAnimation!=null)yAnimation.stop();xRunning=false;yRunning=false;if(lineX!=null)lineX.setVisible(false);if(lineY!=null)lineY.setVisible(false);}
    public void commandFailed() {if(disposed)return;pending=false;if(finished&&canRematch())sessionChanged(Main.socketHandler.sessionState);render(Main.gameViewState.getSnapshot());}
    public void finish(MatchSnapshot s) {
        if(disposed||finished)return;finished=true;pending=false;stopAim();render(s);
        boolean won=java.util.Objects.equals(s.winnerId(),Main.socketHandler.loggedInUserId);
        playEffect(won?"win":"loose");
        String reason=switch(s.endReason()) {case "DISCONNECT"->"Mất kết nối";case "FORFEIT"->"Bỏ cuộc";case "TURN_TIMEOUT"->"Hết thời gian lượt";default->"Về 0 điểm";};
        if(canRematch()){
            if(historyNotice!=null)historyNotice.close();if(settingsNotice!=null)settingsNotice.close();
            resultTitle.setText(won?"Bạn thắng rồi!":"Ván đấu đã kết thúc");
            resultScore.setText(reason+" · "+s.player1Name()+" "+s.player1Score()+" — "+s.player2Score()+" "+s.player2Name());
            boardContainer.setVisible(false);boardContainer.setManaged(false);resultPanel.setVisible(true);resultPanel.setManaged(true);
            sessionChanged(Main.socketHandler.sessionState);updateMediaButtons();
        } else {stopMedia(false);Main.notifications().result(won,reason+" · "+s.player1Name()+" "+s.player1Score()+" — "+s.player2Score()+" "+s.player2Name(),this::goHome);}
    }
    @FXML public void handleBack() {
        if(finished&&!canRematch()){goHome();return;}
        Main.notifications().confirm("Rời phiên chơi?",finished?"Cuộc gọi sẽ kết thúc và bạn sẽ về sảnh.":"Rời lúc này sẽ bị xử thua. Cuộc gọi cũng sẽ kết thúc.","Rời trận",()->{
            if(disposed)return;
            pending=true;stopAim();aimStatusLabel.setDisable(true);
            if(canRematch())Main.socketHandler.leaveSession();else {stopMedia(true);Main.socketHandler.leaveGame();}
        });
    }
    private boolean canRematch(){return Main.socketHandler.sessionState!=null&&Main.socketHandler.sessionState.rematchSupported()&&!"CLOSED".equals(Main.socketHandler.sessionState.phase());}
    private String mediaSessionId(){return Main.socketHandler.sessionIdPresent!=null?Main.socketHandler.sessionIdPresent:Main.gameViewState.getMatchId();}
    @FXML private void toggleRematch(){
        if(disposed||!finished||!canRematch())return;
        var state=Main.socketHandler.sessionState;
        boolean ready=Main.gameViewState.isPlayer1()?state.player1Ready():state.player2Ready();
        rematchButton.setDisable(true);Main.socketHandler.setRematchReady(!ready);
    }
    @FXML private void leaveSession(){if(!disposed&&canRematch()){exitSessionButton.setDisable(true);rematchButton.setDisable(true);Main.socketHandler.leaveSession();}}
    public void sessionChanged(protocol.dto.SessionMessages.State state){
        if(disposed||state==null||!state.rematchSupported()||!finished||!"RESULTS".equals(state.phase()))return;
        boolean ready=Main.gameViewState.isPlayer1()?state.player1Ready():state.player2Ready();
        boolean opponentReady=Main.gameViewState.isPlayer1()?state.player2Ready():state.player1Ready();
        boolean starting=ready&&(Main.gameViewState.getSnapshot().mode()==MatchMode.PVE||opponentReady);
        rematchButton.setDisable(starting);exitSessionButton.setDisable(false);rematchButton.setText(ready?"Hủy sẵn sàng":"Chơi tiếp");
        rematchStatus.setText(starting?"Đang chuẩn bị ván mới…":state.notice()!=null&&state.notice().startsWith("Không")?state.notice():ready?"Đang chờ đối thủ…":opponentReady?"Đối thủ muốn chơi tiếp.":"Bạn có muốn chơi tiếp?");
    }
    public void nextMatch(MatchSnapshot snapshot){
        if(disposed)return;if(historyNotice!=null)historyNotice.close();if(settingsNotice!=null)settingsNotice.close();
        finished=false;pending=false;historyOpen=false;displayedTurn=-1;animatedTurn=-1;turnMotion.clear();myScoreEffect.reset(301);otherScoreEffect.reset(301);
        resultPanel.setVisible(false);resultPanel.setManaged(false);boardContainer.setVisible(true);boardContainer.setManaged(true);
        rematchButton.setDisable(false);exitSessionButton.setDisable(false);render(snapshot);
    }
    public void sessionClosed(String notice){
        if(disposed)return;stopMedia(false);goHome();Main.notifications().toast("Phiên chơi",notice==null?"Phiên chơi đã kết thúc.":notice,"info");
    }
    private void goHome() {if(disposed)return;try{Main.setRoot("home");}catch(java.io.IOException e){Main.socketHandler.showError(e.getMessage());}}
    @FXML public void handleSend() {
        String text=chatInput.getText().trim();if(text.isEmpty()||text.length()>500||(finished&&!canRematch()))return;
        String id=canRematch()?Main.socketHandler.sessionIdPresent:Main.socketHandler.roomIdPresent;
        Main.socketHandler.sendData(Message.of(MessageType.CHAT_MESSAGE,new ChatMessage(id,text,null)));chatInput.clear();
    }
    public void addMessage(String text) {if(!disposed){chatList.getItems().add(text);if(chatList.getItems().size()>300)chatList.getItems().remove(0);chatList.scrollTo(chatList.getItems().size()-1);}}
    private void animateTurn(boolean mine,long turnId) {
        turnMotion.activate(mine);animatedTurn=turnId;lastTurnMine=mine;
    }
    @FXML private void showHistory() {
        if(disposed||(finished&&!canRematch())||historyOpen)return;
        historyOpen=true;stopAim();render(Main.gameViewState.getSnapshot());
        historyRows=new VBox(8);refreshHistory();
        ScrollPane scroll=new ScrollPane(historyRows);scroll.setId("historyScroll");scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);scroll.setPrefViewportHeight(300);scroll.setMaxHeight(440);
        historyNotice=Main.notifications().custom("Lịch sử các lượt",scroll,()->{
            historyOpen=false;historyNotice=null;historyRows=null;
            if(!disposed&&!finished)render(Main.gameViewState.getSnapshot());
        });
    }
    private void refreshHistory() {
        if(historyRows==null)return;
        var mine=Main.gameViewState.history(true);var theirs=Main.gameViewState.history(false);
        historyRows.getChildren().clear();
        if(mine.isEmpty()&&theirs.isEmpty()){Label empty=new Label("Chưa có lượt ném nào.");empty.getStyleClass().add("muted");historyRows.getChildren().add(empty);return;}
        HBox header=new HBox(10,historyColumnTitle("Bạn"),historyColumnTitle("Đối thủ"));historyRows.getChildren().add(header);
        for(int i=0;i<Math.max(mine.size(),theirs.size());i++){
            HBox row=new HBox(10,historyCell(i<mine.size()?mine.get(i):null,i+1),historyCell(i<theirs.size()?theirs.get(i):null,i+1));
            historyRows.getChildren().add(row);
        }
    }
    private Label historyColumnTitle(String text){Label label=new Label(text);label.getStyleClass().add("history-heading");label.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(label,Priority.ALWAYS);return label;}
    private VBox historyCell(models.GameViewState.Turn turn,int number){
        String darts="—   ·   —   ·   —",summary="Chưa có lượt";
        if(turn!=null){
            darts=turn.darts().stream().map(d->d==null?"—":d.bust()?d.score()+" BUST":String.valueOf(d.score())).collect(java.util.stream.Collectors.joining("   ·   "));
            summary="Trừ "+turn.deducted()+"  ·  Còn "+turn.remaining();
        }
        return ui.TurnHistoryView.cell(number,darts,summary);
    }
    @FXML private void showDeviceSettings() {
        if(disposed||(finished&&!canRematch())||!pvpMedia||historyOpen)return;
        historyOpen=true;stopAim();render(Main.gameViewState.getSnapshot());
        if(deviceSettingsPanel==null){
            microphoneChoice.setMaxWidth(Double.MAX_VALUE);speakerChoice.setMaxWidth(Double.MAX_VALUE);cameraChoice.setMaxWidth(Double.MAX_VALUE);
            deviceSettingsPanel=new VBox(8,new Label("Micro"),microphoneChoice,new Label("Loa"),speakerChoice,new Label("Camera"),cameraChoice);
            deviceSettingsPanel.setId("deviceSettingsPanel");
        }
        settingsNotice=Main.notifications().custom("Cài đặt thiết bị",deviceSettingsPanel,()->{settingsNotice=null;historyOpen=false;if(!disposed&&!finished)render(Main.gameViewState.getSnapshot());});
    }
    private Group deviceIcon(String kind,boolean enabled){
        SVGPath path=new SVGPath();
        path.setContent(switch(kind){
            case "camera"->"M2 5 H14 V15 H2 Z M14 8 L20 5 V15 L14 12 Z";
            case "microphone"->"M8 4 C8 1 12 1 12 4 V10 C12 13 8 13 8 10 Z M5 9 C5 16 15 16 15 9 M10 15 V19 M7 19 H13";
            default->"M10 2 L12 2 L13 4 L15 5 L17 4 L19 6 L18 8 L19 10 L18 12 L19 14 L17 16 L15 15 L13 16 L12 18 L10 18 L9 16 L7 15 L5 16 L3 14 L4 12 L3 10 L4 8 L3 6 L5 4 L7 5 L9 4 Z M11 7 A3 3 0 1 0 11 13 A3 3 0 1 0 11 7";
        });
        path.setFill(Color.TRANSPARENT);path.setStroke(Color.web("#76512d"));path.setStrokeWidth(1.6);path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        Group icon=new Group(path);
        if(!enabled&&!kind.equals("settings")){Line slash=new Line(1,19,20,1);slash.setStroke(Color.web("#b65035"));slash.setStrokeWidth(2.2);icon.getChildren().add(slash);}
        return icon;
    }
    private void deviceButton(Button button,String kind,boolean enabled,boolean requested,String label){
        boolean waiting=requested&&!enabled;
        button.setGraphic(deviceIcon(kind,enabled));button.pseudoClassStateChanged(ACTIVE,enabled);button.pseudoClassStateChanged(PENDING,waiting);
        String action=waiting?"Hủy bật "+label:enabled?"Tắt "+label:"Bật "+label;
        button.setAccessibleText(action);button.setTooltip(new Tooltip(action+(waiting?" (đang kết nối)":"")));button.setDisable((finished&&!canRematch())||!pvpMedia);
    }
    private void updateMediaButtons(){
        deviceButton(microphoneButton,"microphone",microphoneEnabled,microphoneRequested,"mic");
        deviceButton(cameraButton,"camera",cameraEnabled,cameraRequested,"camera");
        deviceSettingsButton.setDisable((finished&&!canRematch())||!pvpMedia);
    }
    private void playDartImpact() {
        if(disposed||!effects)return;
        try {if(dartImpact!=null)dartImpact.play();else playEffect("dart");}
        catch(RuntimeException e) {System.err.println("Impact audio unavailable: "+e.getMessage());}
    }
    private void updateMediaAvailability() {
        MatchSnapshot snapshot=Main.gameViewState.getSnapshot();pvpMedia=snapshot!=null&&snapshot.mode()==MatchMode.PVP;
        mediaControls.setVisible(pvpMedia);mediaControls.setManaged(pvpMedia);mediaStatusLabel.setVisible(pvpMedia);mediaStatusLabel.setManaged(pvpMedia);
        localVideoFrame.setVisible(pvpMedia);localVideoFrame.setManaged(pvpMedia);remoteVideoFrame.setVisible(pvpMedia);remoteVideoFrame.setManaged(pvpMedia);
        chatPanel.setVisible(pvpMedia);chatPanel.setManaged(pvpMedia);
        chatComposer.setVisible(pvpMedia);chatComposer.setManaged(pvpMedia);
        if(!pvpMedia){
            remoteVideoPlaceholder.setText("BOT");
            if(snapshot!=null&&remoteVideoPlaceholder.getGraphic()==null){
                Rectangle face=new Rectangle(44,34,Color.web("#e9d6a1"));face.setArcWidth(10);face.setArcHeight(10);
                Circle eye1=new Circle(13,15,3,Color.web("#40552f")),eye2=new Circle(31,15,3,Color.web("#40552f"));
                Line mouth=new Line(14,25,30,25);mouth.setStroke(Color.web("#40552f"));mouth.setStrokeWidth(2);
                remoteVideoPlaceholder.setGraphic(new Group(face,eye1,eye2,mouth));remoteVideoPlaceholder.setContentDisplay(ContentDisplay.TOP);
            }
        } else {remoteVideoPlaceholder.setText("Camera đối thủ đang tắt");remoteVideoPlaceholder.setGraphic(null);}
    }
    private void joinCall() {
        if(disposed||(finished&&!canRematch())||!pvpMedia||callJoined)return;
        String matchId=mediaSessionId();if(matchId==null)return;
        if(activeCallId!=null)previousCallId=activeCallId;activeCallId=null;
        callJoined=true;peerInCall=false;mediaStatusLabel.setText("Đang kết nối mic/video…");
        mediaCall.join(matchId);
    }
    @FXML private void toggleMicrophone() {
        if(disposed||(finished&&!canRematch())||!pvpMedia)return;microphoneRequested=!microphoneRequested;updateMediaButtons();
        if(!callJoined&&microphoneRequested)joinCall();
        if(peerInCall)mediaCall.setMicrophone(microphoneRequested,microphoneChoice.getSelectionModel().getSelectedIndex()-1);
    }
    @FXML private void toggleCamera() {
        if(disposed||(finished&&!canRematch())||!pvpMedia)return;cameraRequested=!cameraRequested;updateMediaButtons();
        if(!callJoined&&cameraRequested)joinCall();
        if(peerInCall)mediaCall.setCamera(cameraRequested,cameraChoice.getSelectionModel().getSelectedIndex()-1);
    }
    public void mediaPeerState(protocol.dto.MediaMessages.PeerState state) {
        if(disposed||(finished&&!canRematch())||!java.util.Objects.equals(mediaSessionId(),state.matchId()))return;
        boolean ended=state.notice()!=null&&(state.notice().startsWith("Cuộc gọi đã kết thúc")||state.notice().startsWith("Trận đã kết thúc"));
        if(ended&&callJoined&&state.callId()!=null&&state.callId().equals(previousCallId))return;
        if(ended&&activeCallId!=null&&!activeCallId.equals(state.callId()))return;
        mediaCall.onPeerState(state);
        if(state.callId()==null||state.callId().isBlank()||ended) {
            if(activeCallId!=null)previousCallId=activeCallId;activeCallId=null;
            callJoined=false;peerInCall=false;microphoneEnabled=false;cameraEnabled=false;microphoneRequested=false;cameraRequested=false;
            if(!ended)mediaCall.cancel();
            remoteVideoExpiry.stop();localVideoView.setImage(null);remoteVideoView.setImage(null);localVideoPlaceholder.setVisible(true);remoteVideoPlaceholder.setVisible(true);
            mediaStatusLabel.setText(state.notice()==null?"Không thể kết nối mic/video.":state.notice());
            updateMediaButtons();
        } else {
            activeCallId=state.callId();callJoined=true;boolean becameReady=state.peerJoined()&&!peerInCall;peerInCall=state.peerJoined();
            if(becameReady){
                if(microphoneRequested)mediaCall.setMicrophone(true,microphoneChoice.getSelectionModel().getSelectedIndex()-1);
                if(cameraRequested)mediaCall.setCamera(true,cameraChoice.getSelectionModel().getSelectedIndex()-1);
            }
            updateMediaButtons();
        }
    }
    public void mediaIceServers(protocol.dto.MediaMessages.IceConfig config) {
        if(disposed||(finished&&!canRematch())||!callJoined||config==null||!java.util.Objects.equals(mediaSessionId(),config.matchId())
                ||config.callId()==null||config.callId().equals(previousCallId)||(activeCallId!=null&&!activeCallId.equals(config.callId())))return;
        activeCallId=config.callId();mediaCall.onIceConfig(config);
    }
    public void mediaDescription(protocol.dto.MediaMessages.Signal signal) {if(!disposed&&callJoined)mediaCall.onDescription(signal);}
    public void mediaCandidate(protocol.dto.MediaMessages.Signal signal) {if(!disposed&&callJoined)mediaCall.onCandidate(signal);}
    public void mediaRequestFailed(String message) {
        if(disposed||!callJoined)return;
        mediaCall.cancel();
        if(activeCallId!=null)previousCallId=activeCallId;activeCallId=null;
        callJoined=false;peerInCall=false;microphoneEnabled=false;cameraEnabled=false;microphoneRequested=false;cameraRequested=false;
        remoteVideoExpiry.stop();
        localVideoView.setImage(null);remoteVideoView.setImage(null);localVideoPlaceholder.setVisible(true);remoteVideoPlaceholder.setVisible(true);
        updateMediaButtons();mediaStatusLabel.setText(message);
    }
    public void mediaDisconnected() {stopMedia(false);}
    private void stopMedia(boolean notifyServer) {
        if(mediaCall!=null){if(notifyServer)mediaCall.leave();else mediaCall.close(false);}
        if(!notifyServer){previousCallId=activeCallId;activeCallId=null;}
        callJoined=false;peerInCall=false;microphoneEnabled=false;cameraEnabled=false;microphoneRequested=false;cameraRequested=false;
        remoteVideoExpiry.stop();
        updateMediaButtons();
        if(localVideoView!=null)localVideoView.setImage(null);if(remoteVideoView!=null)remoteVideoView.setImage(null);
        if(localVideoPlaceholder!=null)localVideoPlaceholder.setVisible(true);if(remoteVideoPlaceholder!=null)remoteVideoPlaceholder.setVisible(true);
    }
    public void dispose() {if(mediaCall!=null)mediaCall.close(!finished&&Main.socketHandler!=null&&Main.socketHandler.isConnected());disposed=true;remoteVideoExpiry.stop();if(turnMotion!=null)turnMotion.clear();stopAim();rotationEffect.cancel();dartEffects.dispose();if(myScoreEffect!=null)myScoreEffect.dispose();if(otherScoreEffect!=null)otherScoreEffect.dispose();if(dartImpact!=null)dartImpact.stop();players.forEach(javafx.scene.media.MediaPlayer::dispose);players.clear();}
    @FXML private void toggleSound() {sound=!sound;soundButton.setText(sound?"Nhạc: bật":"Nhạc: tắt");if(sound)Main.playBackgroundMusic();else Main.stopBackgroundMusic();}
    @FXML private void toggleEffectSound() {effects=!effects;if(!effects&&dartImpact!=null)dartImpact.stop();effectSoundButton.setText(effects?"Hiệu ứng: bật":"Hiệu ứng: tắt");}
}
