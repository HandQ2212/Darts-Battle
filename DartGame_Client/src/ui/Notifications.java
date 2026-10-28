package ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.util.*;
import java.util.function.Consumer;

/** In-window notifications. Only explicit user actions invoke gameplay callbacks. */
public final class Notifications {
    private final StackPane root=new StackPane(), content=new StackPane(), modal=new StackPane();
    private final VBox rail=new VBox(12);
    private final List<Notice> toasts=new ArrayList<>();
    private Notice activeModal, invitation;
    private Parent page;

    public Notifications() {
        root.getStylesheets().add(getClass().getResource("/views/theme.css").toExternalForm());
        root.setId("notificationHost");
        rail.setId("notificationRail");rail.setPickOnBounds(false);
        rail.setMaxSize(380,Region.USE_PREF_SIZE);
        StackPane.setAlignment(rail,Pos.TOP_RIGHT);StackPane.setMargin(rail,new Insets(22));
        modal.setId("modalLayer");modal.getStyleClass().add("notification-scrim");modal.setVisible(false);
        root.getChildren().addAll(content,rail,modal);
    }
    public StackPane root() { return root; }
    public void setPage(Parent value) {
        if(activeModal!=null)activeModal.close();
        page=value;content.getChildren().setAll(value);
    }
    public void error(String message) {
        message=Objects.requireNonNullElse(message,"Vui lòng thử lại.");
        Node field=page==null?null:page.lookup("#formErrorLabel");
        if(field instanceof Label label) {
            label.setText(message);label.setVisible(true);label.setManaged(true);
        } else toast("Có điều cần chú ý",message,"error");
    }
    public void toast(String title,String message,String kind) {
        message=Objects.requireNonNullElse(message,"Vui lòng thử lại.");
        // Deduplicate repeated network/server errors and cap the visible queue.
        for(Notice notice:toasts)if(message.equals(notice.message))return;
        if(toasts.size()>=3)toasts.get(0).close();
        VBox card=card(kind.equals("error")?"!":kind.equals("success")?"✓":"i",title,message,kind);
        card.setId("toast");
        Notice notice=new Notice(card);notice.message=message;toasts.add(notice);
        Button dismiss=new Button("×");dismiss.getStyleClass().add("notice-close");dismiss.setAccessibleText("Đóng thông báo");
        dismiss.setOnAction(event->notice.close());
        ((HBox)card.getChildren().get(0)).getChildren().add(dismiss);
        rail.getChildren().add(card);
        PauseTransition expiry=new PauseTransition(Duration.seconds(kind.equals("error")?6:4));
        expiry.setOnFinished(event->notice.close());notice.animations.add(expiry);
        card.setOnMouseEntered(event->expiry.pause());card.setOnMouseExited(event->{if(!notice.closed)expiry.play();});
        appear(card,notice);expiry.play();
    }
    public Notice confirm(String title,String message,String action,Runnable confirmed) {
        return dialog("BẠN QUYẾT ĐỊNH",title,message,action,confirmed,true,"confirm");
    }
    public Notice info(String title,String message) {
        return dialog("DARTS BATTLE",title,message,"Đã hiểu",()->{},false,"info");
    }
    public Notice result(boolean won,String message,Runnable goHome) {
        return dialog(won?"★  CHIẾN THẮNG":"HẸN TRẬN TIẾP THEO",won?"Bạn thắng rồi!":"Một trận đấu đáng nhớ",message,"Về sảnh",goHome,false,won?"victory":"result");
    }
    private Notice dialog(String badge,String title,String message,String action,Runnable confirmed,boolean cancellable,String kind) {
        if(activeModal!=null)activeModal.close();
        Node previousFocus=root.getScene()==null?null:root.getScene().getFocusOwner();
        VBox card=card(badge,title,"",kind);card.getStyleClass().add("notification-modal");
        card.setId("notificationDialog");card.setMaxSize(520,Region.USE_PREF_SIZE);
        Label body=label(message,"notice-body");body.setMinHeight(Region.USE_PREF_SIZE);
        ScrollPane scroll=new ScrollPane(body);scroll.setFitToWidth(true);scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("notice-scroll");scroll.setPrefViewportHeight(message.length()>400?250:72);scroll.setMaxHeight(280);
        card.getChildren().add(scroll);
        HBox actions=new HBox(12);actions.setAlignment(Pos.CENTER_RIGHT);
        Button accept=new Button(action);accept.setId("noticeConfirm");accept.getStyleClass().add("primary");
        Notice notice=new Notice(card);activeModal=notice;
        List<Button> buttons=new ArrayList<>();
        if(cancellable) {
            Button cancel=new Button("Ở lại");cancel.setId("noticeCancel");cancel.getStyleClass().add("notice-secondary");
            cancel.setOnAction(event->notice.close());actions.getChildren().add(cancel);buttons.add(cancel);
        }
        accept.setOnAction(event->{if(!notice.closed){notice.close();confirmed.run();}});
        actions.getChildren().add(accept);buttons.add(accept);card.getChildren().add(actions);
        notice.afterClose=()->{
            content.setDisable(false);modal.setVisible(false);modal.getChildren().clear();modal.setOnKeyPressed(null);
            if(previousFocus!=null&&previousFocus.getScene()==root.getScene())previousFocus.requestFocus();
        };
        modal.setOnKeyPressed(event->{
            if(event.getCode()==KeyCode.ESCAPE) {
                if(cancellable)notice.close();else accept.fire();event.consume();
            } else if(event.getCode()==KeyCode.TAB) {
                Node focus=root.getScene().getFocusOwner();int index=buttons.indexOf(focus);
                buttons.get(Math.floorMod(index+(event.isShiftDown()?-1:1),buttons.size())).requestFocus();event.consume();
            } else if(event.getCode()==KeyCode.ENTER && root.getScene().getFocusOwner() instanceof Button button) {
                button.fire();event.consume();
            }
        });
        content.setDisable(true);modal.getChildren().setAll(card);modal.setVisible(true);
        appear(card,notice);
        // Focus the safe choice; notices and invitations never move keyboard focus.
        buttons.get(0).requestFocus();
        return notice;
    }
    public Notice invite(String name,long expiresAt,Consumer<Boolean> answer) {
        if(invitation!=null)invitation.close();
        VBox card=card("THÁCH ĐẤU",name+" mời bạn chơi","Một trận phi tiêu đang chờ bạn. Sẵn sàng vào sân?","invite");
        card.setId("invitationCard");
        Label countdown=label("","notice-countdown");countdown.setId("inviteCountdown");
        ProgressBar progress=new ProgressBar(1);progress.setMaxWidth(Double.MAX_VALUE);
        Button reject=new Button("Để lần sau"),accept=new Button("Vào trận");
        reject.setId("inviteReject");accept.setId("inviteAccept");accept.getStyleClass().add("primary");reject.getStyleClass().add("notice-secondary");
        HBox actions=new HBox(8,reject,accept);actions.setAlignment(Pos.CENTER_RIGHT);
        card.getChildren().addAll(countdown,progress,actions);
        Notice notice=new Notice(card);invitation=notice;
        Consumer<Boolean> respond=value->{
            if(notice.closed)return;
            boolean valid=System.currentTimeMillis()<expiresAt;notice.close();
            if(valid)answer.accept(value);
        };
        accept.setOnAction(event->respond.accept(true));reject.setOnAction(event->respond.accept(false));
        long total=Math.max(1,expiresAt-System.currentTimeMillis());
        Runnable tick=()->{
            long left=expiresAt-System.currentTimeMillis();
            if(left<=0){notice.close();toast("Lời mời đã hết hạn","Bạn có thể gửi lời mời mới từ danh sách người chơi.","info");return;}
            countdown.setText("Còn "+((left+999)/1000)+" giây để trả lời");progress.setProgress(Math.min(1,(double)left/total));
        };
        Timeline timer=new Timeline(new KeyFrame(Duration.millis(200),event->tick.run()));timer.setCycleCount(Animation.INDEFINITE);
        notice.animations.add(timer);rail.getChildren().add(0,card);tick.run();
        if(!notice.closed){appear(card,notice);timer.play();}
        return notice;
    }
    private Label label(String text,String style) {
        Label label=new Label(text);label.getStyleClass().add(style);label.setWrapText(true);label.setMaxWidth(Double.MAX_VALUE);return label;
    }
    private VBox card(String badge,String title,String message,String kind) {
        Label eyebrow=label(badge,"notice-badge");Region gap=new Region();HBox.setHgrow(gap,Priority.ALWAYS);
        HBox header=new HBox(8,eyebrow,gap);header.setAlignment(Pos.CENTER_LEFT);
        VBox card=new VBox(10,header,label(title,"notice-title"));card.getStyleClass().addAll("notification-card","notice-"+kind);
        if(!message.isBlank())card.getChildren().add(label(message,"notice-body"));
        card.setMinWidth(0);return card;
    }
    private void appear(Node node,Notice notice) {
        FadeTransition fade=new FadeTransition(Duration.millis(180),node);fade.setFromValue(0);fade.setToValue(1);
        TranslateTransition slide=new TranslateTransition(Duration.millis(220),node);slide.setFromY(-10);slide.setToY(0);slide.setInterpolator(Interpolator.EASE_OUT);
        notice.animations.add(fade);notice.animations.add(slide);fade.play();slide.play();
    }
    public void dispose() {
        if(activeModal!=null)activeModal.close();if(invitation!=null)invitation.close();
        for(Notice notice:new ArrayList<>(toasts))notice.close();
    }
    public final class Notice implements AutoCloseable {
        private final Node node;
        private final List<Animation> animations=new ArrayList<>();
        private boolean closed;
        private String message;
        private Runnable afterClose=()->{};
        private Notice(Node value){node=value;}
        @Override public void close() {
            if(closed)return;closed=true;
            animations.forEach(Animation::stop);node.setOpacity(1);node.setTranslateY(0);
            rail.getChildren().remove(node);toasts.remove(this);
            if(invitation==this)invitation=null;
            if(activeModal==this)activeModal=null;
            afterClose.run();
        }
    }
}
