import btl_ltm_n3.Main;
import ui.Notifications;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.util.concurrent.*;

/** Checks callbacks, focus, form errors and expiry on real JavaFX controls. */
public class NotificationsTest {
    private static Notifications notices;
    private static Stage stage;
    private static Parent login;
    private static int accepted,rejected,confirmed;
    private static <T> T fx(Callable<T> action) throws Exception {
        CompletableFuture<T> result=new CompletableFuture<>();
        Platform.runLater(()->{try{result.complete(action.call());}catch(Throwable e){result.completeExceptionally(e);}});
        return result.get(15,TimeUnit.SECONDS);
    }
    private static void check(boolean condition,String message){ClientUiSmokeTest.check(condition,message);}
    private static Button button(String id){return (Button)notices.root().lookup(id);}
    private static void preview(String name)throws Exception{ClientUiSmokeTest.preview(notices.root(),name,1080,780);}
    public static void main(String[] args)throws Exception {
        CompletableFuture<Void> ready=new CompletableFuture<>();Platform.startup(()->ready.complete(null));ready.get(10,TimeUnit.SECONDS);
        try {
            fx(()->{
                Main.socketHandler=new ClientUiSmokeTest.FakeSocket();
                login=FXMLLoader.load(Main.class.getResource("/views/login.fxml"));
                notices=Main.notifications();notices.setPage(login);stage=new Stage();stage.setScene(new Scene(notices.root(),1080,780));stage.show();
                TextField username=(TextField)login.lookup("#usernameField");username.requestFocus();
                int windows=Window.getWindows().size();
                notices.toast("Sẵn sàng vào sân","Đã tạo tài khoản. Chúc bạn có một trận đấu vui!","success");
                check(Window.getWindows().size()==windows,"toast opens no native popup window");
                check(stage.getScene().getFocusOwner()==username,"toast preserves typing focus");
                Main.socketHandler.showError("Tên đăng nhập hoặc mật khẩu chưa đúng.");
                Label error=(Label)login.lookup("#formErrorLabel");
                check(error.isVisible()&&error.isManaged()&&error.getText().contains("mật khẩu"),"authentication error stays in form");
                preview("notification-form-error");
                username.setText("new_name");check(!error.isVisible()&&!error.isManaged(),"editing clears form error");
                notices.confirm("Rời trận đấu?","Rời lúc này sẽ bị xử thua. Bạn vẫn muốn về sảnh?","Rời trận",()->confirmed++);
                check(login.isDisabled(),"confirmation blocks background game input");
                check(Window.getWindows().size()==windows,"confirmation remains inside the game window");
                preview("notification-confirm");button("#noticeCancel").fire();
                check(confirmed==0&&!login.isDisabled(),"cancel restores page without sending action");
                notices.confirm("Đăng xuất?","Bạn muốn đăng xuất?","Đăng xuất",()->confirmed++);
                Button accept=button("#noticeConfirm");accept.fire();accept.fire();
                check(confirmed==1,"confirmation callback fires exactly once");
                notices.confirm("Rời trận?","Không tự gửi lệnh khi thay màn hình.","Rời trận",()->confirmed++);
                notices.setPage(login);check(confirmed==1&&!login.isDisabled(),"navigation silently cancels stale confirmation");
                int[] customClosed={0};ComboBox<String> device=new ComboBox<>();device.getItems().add("Mặc định");
                notices.custom("Thiết bị",new VBox(device),()->customClosed[0]++);
                check(login.isDisabled(),"custom modal blocks the page");
                javafx.event.Event.fireEvent(notices.root().lookup("#modalLayer"),new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,"","",javafx.scene.input.KeyCode.ESCAPE,false,false,false,false));
                check(customClosed[0]==1&&!login.isDisabled(),"Escape closes custom modal and restores page");
                notices.invite("Bobby",System.currentTimeMillis()+30000,value->{if(value)accepted++;else rejected++;});
                check(!login.isDisabled(),"invitation does not block page or chat");
                check(notices.root().lookup("#inviteCountdown")!=null,"invitation shows countdown");
                preview("notification-invitation");
                Button join=button("#inviteAccept");join.fire();join.fire();check(accepted==1&&rejected==0,"accept invitation once");
                notices.invite("Carol",System.currentTimeMillis()+30000,value->{if(value)accepted++;else rejected++;});
                button("#inviteReject").fire();check(rejected==1,"explicit reject sends one response");
                var silent=notices.invite("David",System.currentTimeMillis()+30000,value->{if(value)accepted++;else rejected++;});silent.close();
                check(accepted==1&&rejected==1,"programmatic invitation close never sends a response");
                notices.invite("Expired",System.currentTimeMillis()+150,value->{if(value)accepted++;else rejected++;});
                return null;
            });
            Thread.sleep(400);
            fx(()->{
                check(notices.root().lookup("#invitationCard")==null&&accepted==1&&rejected==1,"expired invitation removes itself without accepting or rejecting");
                notices.result(true,"Về 0 điểm · Alice 0 — 151 Bobby",()->confirmed++);
                preview("notification-victory");
                Label resultTitle=(Label)notices.root().lookup(".notification-modal .notice-title");
                check(!resultTitle.getFont().getFamily().equals("Lilita One"),"Vietnamese result title uses system font without mixed glyph fallback");
                button("#noticeConfirm").fire();check(confirmed==2,"result returns to lobby only on dismissal");
                notices.toast("Một thông báo","Nội dung không bị lặp lại.","info");notices.toast("Một thông báo","Nội dung không bị lặp lại.","info");
                VBox rail=(VBox)notices.root().lookup("#notificationRail");
                long count=rail.getChildren().stream().filter(n->n instanceof VBox box&&box.getChildren().stream().anyMatch(child->child instanceof Label label&&label.getText().equals("Nội dung không bị lặp lại."))).count();
                check(count==1,"repeated notifications are deduplicated");
                notices.dispose();notices.toast("Thông báo ngắn","Thẻ sẽ tự ẩn.","success");
                // Keep the real desktop pointer from pausing this expiry check on hover.
                var toast=notices.root().lookup("#toast");toast.setMouseTransparent(true);toast.getOnMouseExited().handle(null);return null;
            });
            Thread.sleep(4300);
            fx(()->{check(notices.root().lookup("#toast")==null,"short notices expire automatically");return null;});
            System.out.println("ALL NOTIFICATION CHECKS PASSED");
        } finally {
            fx(()->{if(notices!=null)notices.dispose();if(Main.socketHandler!=null)Main.socketHandler.shutdown();if(stage!=null)stage.hide();return null;});Platform.exit();
        }
    }
}
