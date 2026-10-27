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
import javafx.util.Duration;

/** Rendering and aiming only: all scores, turns and phase come from the server. */
public class StartGameController {
    @FXML private ListView<String> chatList;
    @FXML private TextField chatInput;
    @FXML private Pane gamePane, scoreboardPane;
    @FXML private Button soundButton, effectSoundButton;
    @FXML private Label aimStatusLabel;
    @FXML private Label instructionLabel, phaseLabel, turnLabel, lastThrowLabel, lastThrowScoreLabel;
    @FXML private VBox botTurnOverlay;
    @FXML private BorderPane rootPane;
    private static final double CX=225,CY=200;
    private final Group board=new Group(), marks=new Group();
    private ui.DartThrowEffects dartEffects;
    private ui.BoardRotationEffect rotationEffect;
    private javafx.scene.media.AudioClip dartImpact;
    private final Text myScore=new Text(),otherScore=new Text();
    private final Text[] mine=new Text[3],theirs=new Text[3];
    private VBox myCard, otherCard;
    private final Label myTurnTag=new Label(), otherTurnTag=new Label();
    private static final javafx.css.PseudoClass ACTIVE=javafx.css.PseudoClass.getPseudoClass("active");
    private static final javafx.css.PseudoClass BUST=javafx.css.PseudoClass.getPseudoClass("bust");
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
        myCard=playerCard(Main.socketHandler.loginUser,"BẠN",myScore,mine,myTurnTag);
        otherCard=playerCard(Main.socketHandler.competitor,"ĐỐI THỦ",otherScore,theirs,otherTurnTag);
        otherCard.getStyleClass().add("opponent-card");
        Label versus=new Label("VS");versus.getStyleClass().add("versus");
        ((HBox)scoreboardPane).setAlignment(javafx.geometry.Pos.CENTER);
        scoreboardPane.getChildren().addAll(myCard,versus,otherCard);
        chatList.setCellFactory(list->new ListCell<>() {
            private final Label message=new Label();
            {message.setWrapText(true);message.maxWidthProperty().bind(list.widthProperty().subtract(36));}
            @Override protected void updateItem(String item,boolean empty) {
                super.updateItem(item,empty);setText(null);message.setText(empty?null:item);setGraphic(empty?null:message);
            }
        });
        chatInput.setOnAction(e->handleSend());
        aimStatusLabel.setDisable(true);
        render(Main.gameViewState.getSnapshot());
    }
    private VBox playerCard(String name,String caption,Text score,Text[] darts,Label tag) {
        Label player=new Label(name==null?caption:name);player.getStyleClass().add("player-name");
        player.setMaxWidth(160);tag.getStyleClass().add("turn-tag");
        Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);
        HBox header=new HBox(8,player,spacer,tag);header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        score.getStyleClass().add("score-value");
        Label remaining=new Label(caption+" / CÒN LẠI");remaining.getStyleClass().add("score-caption");
        VBox value=new VBox(0,score,remaining);HBox chips=new HBox(5);chips.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        for(int i=0;i<3;i++) {
            darts[i]=new Text("–");darts[i].getStyleClass().add("dart-value");
            StackPane chip=new StackPane(darts[i]);chip.getStyleClass().add("dart-chip");
            final Text dart=darts[i];dart.textProperty().addListener((o,old,text)->chip.pseudoClassStateChanged(BUST,"BUST".equals(text)));
            chips.getChildren().add(chip);
        }
        Region gap=new Region();HBox.setHgrow(gap,Priority.ALWAYS);
        HBox detail=new HBox(6,value,gap,chips);detail.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox card=new VBox(4,header,detail);card.getStyleClass().add("score-card");
        card.setMinWidth(0);card.setPrefWidth(300);card.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(card,Priority.ALWAYS);
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
        if(displayedTurn!=s.turnId()) {
            rotationEffect.cancel();
            stopAim();pending=false;dartEffects.beginTurn();displayedTurn=s.turnId();
            Text[] row=Main.gameViewState.isMyTurn()?mine:theirs;for(Text t:row)t.setText("–");
            if(s.status()==MatchStatus.WAITING_ROTATION)rotationEffect.play(s.rotationDegree(),()->{
                if(!disposed && displayedTurn==s.turnId())render(Main.gameViewState.getSnapshot());
            });
        }
        if(s.status()==MatchStatus.FINISHED)rotationEffect.cancel();
        rotationEffect.setAngle(s.rotationDegree());
        boolean rotating=rotationEffect.isRunning() || s.status()==MatchStatus.WAITING_ROTATION;
        boolean p1=Main.gameViewState.isPlayer1();
        myScore.setText(String.valueOf(p1?s.player1Score():s.player2Score()));otherScore.setText(String.valueOf(p1?s.player2Score():s.player1Score()));
        boolean me=Main.gameViewState.isMyTurn() && s.status()!=MatchStatus.FINISHED;
        boolean active=s.status()!=MatchStatus.FINISHED;
        myCard.pseudoClassStateChanged(ACTIVE,me);otherCard.pseudoClassStateChanged(ACTIVE,active&&!me);
        myTurnTag.setText(me?"LƯỢT BẠN":"");otherTurnTag.setText(active&&!me?"ĐẾN LƯỢT":"");
        turnLabel.setText(!active?"Trận đấu kết thúc":me?"Đến lượt bạn.":"Lượt của đối thủ.");
        phaseLabel.setText(!active?"HOÀN THÀNH":rotating?"01 / TỰ ĐỘNG XOAY BẢNG":!me?"CHỜ ĐỐI THỦ":pending?"ĐANG XÁC NHẬN":"02 / NGẮM & NÉM");
        if(!me || rotating || s.status()!=MatchStatus.PLAYING || pending)aimStatusLabel.setText(rotating?"Đang xoay...":pending?"Đang gửi...":"Chờ lượt");
        botTurnOverlay.setVisible(!rotating && s.actorType()==ActorType.BOT && s.status()!=MatchStatus.FINISHED);
        if(!me || rotating || s.status()!=MatchStatus.PLAYING)stopAim();
        aimStatusLabel.setDisable(!me || rotating || s.status()!=MatchStatus.PLAYING || pending || s.dartsThrown()>=3);
        gamePane.setCursor(aimStatusLabel.isDisabled()?javafx.scene.Cursor.DEFAULT:javafx.scene.Cursor.CROSSHAIR);
        if(s.status()==MatchStatus.FINISHED) {instructionLabel.setText("Trận đấu đã kết thúc.");return;}
        if(rotating)instructionLabel.setText("Bảng đang tự động xoay — chờ bảng dừng để ném.");
        else if(!me)instructionLabel.setText("Đang chờ "+(s.actorId()==null?"BOT":s.actorId()==s.player1Id()?s.player1Name():s.player2Name()));
        else if(pending)instructionLabel.setText("Đang chờ server xác nhận...");
        else if(s.dartsThrown()<3) {instructionLabel.setText("Phi tiêu "+(s.dartsThrown()+1)+"/3 — click bảng để chốt X, rồi click lần nữa để ném.");if(!xRunning&&!yRunning)startX();}
    }
    public void showThrow(ThrowResolvedResponse res) {
        if(disposed)return;pending=false;stopAim();
        boolean own=java.util.Objects.equals(res.getActorId(),Main.socketHandler.loggedInUserId);
        dartEffects.throwDart(CX+res.getX(),CY+res.getY(),own,res.getDartScore(),res.isBust());
        lastThrowScoreLabel.setText(String.valueOf(res.getDartScore()));
        lastThrowScoreLabel.pseudoClassStateChanged(BUST,res.isBust());
        lastThrowLabel.setText((own?"Bạn":"Đối thủ")+" · "+res.getDartScore()+" điểm"+(res.isBust()?" · BUST, không trừ điểm":""));
        lastThrowLabel.pseudoClassStateChanged(BUST,res.isBust());
        Text[] row=java.util.Objects.equals(res.getActorId(),Main.socketHandler.loggedInUserId)?mine:theirs;
        if(res.getDartIndex()>=1&&res.getDartIndex()<=3)row[res.getDartIndex()-1].setText(res.isBust()?"BUST":String.valueOf(res.getDartScore()));
        render(res.getSnapshot());
    }
    public void rotationConfirmed(MatchSnapshot s) {pending=false;render(s);}
    private void startX() {
        xRunning=true;lineX.setVisible(true);lineX.setStartX(0);lineX.setEndX(0);aimStatusLabel.setText("Click để chốt X");
        xAnimation=new Timeline(new KeyFrame(Duration.millis(25),e->{double x=(lineX.getStartX()+4)%450;lineX.setStartX(x);lineX.setEndX(x);}));
        xAnimation.setCycleCount(Animation.INDEFINITE);xAnimation.play();
    }
    @FXML public void handleGameAction() {
        if(disposed||pending||rotationEffect.isRunning()||!Main.gameViewState.isMyTurn()
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
    public void commandFailed() {if(disposed)return;pending=false;render(Main.gameViewState.getSnapshot());}
    public void finish(MatchSnapshot s) {
        if(disposed||finished)return;finished=true;pending=false;stopAim();render(s);
        boolean won=java.util.Objects.equals(s.winnerId(),Main.socketHandler.loggedInUserId);
        playEffect(won?"win":"loose");
        String reason=switch(s.endReason()) {case "DISCONNECT"->"Mất kết nối";case "FORFEIT"->"Bỏ cuộc";case "TURN_TIMEOUT"->"Hết thời gian lượt";default->"Về 0 điểm";};
        Main.notifications().result(won,reason+" · "+s.player1Name()+" "+s.player1Score()+" — "+s.player2Score()+" "+s.player2Name(),this::goHome);
    }
    @FXML public void handleBack() {
        if(finished){goHome();return;}
        Main.notifications().confirm("Rời trận đấu?","Rời lúc này sẽ bị xử thua. Bạn vẫn muốn về sảnh?","Rời trận",()->{
            if(disposed||finished)return;
            pending=true;stopAim();aimStatusLabel.setDisable(true);Main.socketHandler.leaveGame();
        });
    }
    private void goHome() {if(disposed)return;try{Main.setRoot("home");}catch(java.io.IOException e){Main.socketHandler.showError(e.getMessage());}}
    @FXML public void handleSend() {
        String text=chatInput.getText().trim();if(text.isEmpty()||text.length()>500||finished)return;
        Main.socketHandler.sendData(Message.of(MessageType.CHAT_MESSAGE,new ChatMessage(Main.socketHandler.roomIdPresent,text,null)));chatInput.clear();
    }
    public void addMessage(String text) {if(!disposed){chatList.getItems().add(text);if(chatList.getItems().size()>300)chatList.getItems().remove(0);chatList.scrollTo(chatList.getItems().size()-1);}}
    private void playDartImpact() {
        if(disposed||!effects)return;
        try {if(dartImpact!=null)dartImpact.play();else playEffect("dart");}
        catch(RuntimeException e) {System.err.println("Impact audio unavailable: "+e.getMessage());}
    }
    public void dispose() {disposed=true;stopAim();rotationEffect.cancel();dartEffects.dispose();if(dartImpact!=null)dartImpact.stop();players.forEach(javafx.scene.media.MediaPlayer::dispose);players.clear();}
    @FXML private void toggleSound() {sound=!sound;soundButton.setText(sound?"Nhạc: bật":"Nhạc: tắt");if(sound)Main.playBackgroundMusic();else Main.stopBackgroundMusic();}
    @FXML private void toggleEffectSound() {effects=!effects;if(!effects&&dartImpact!=null)dartImpact.stop();effectSoundButton.setText(effects?"Hiệu ứng: bật":"Hiệu ứng: tắt");}
}
