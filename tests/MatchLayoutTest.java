import btl_ltm_n3.Main;
import controllers.*;
import enums.BotDifficulty;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import protocol.Message;
import protocol.MessageType;
import protocol.dto.BotMatchRequest;
import protocol.dto.HistoryMessages.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the actual modal actions and rendered column geometry. */
public class MatchLayoutTest {
    static class FakeSocket extends SocketHandler {
        Message last;
        @Override public void sendData(Message m){last=m;}
        @Override public void ensureConnected(Runnable ready){}
    }
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);System.out.println("PASS "+label);}
    public static void main(String[] args)throws Exception{
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
        Platform.startup(()->{
            FakeSocket socket=new FakeSocket();socket.loginUser="Alice";socket.loggedInUserId=1;Main.socketHandler=socket;
            try{
                FXMLLoader loader=new FXMLLoader(Main.class.getResource("/views/home.fxml"));Parent home=loader.load();HomeController controller=loader.getController();
                Main.notifications().setPage(home);new Scene(Main.notifications().root(),1040,780);
                for(BotDifficulty difficulty:BotDifficulty.values()){
                    socket.last=null;controller.handleClickStartGameWithBot();
                    Region modal=(Region)Main.notifications().root().lookup("#customDialog");
                    check(modal!=null&&modal.getMaxWidth()==390,"bot popup is narrow");
                    check(modal.lookupAll(".combo-box").isEmpty(),"bot choices are separate buttons");
                    Button button=(Button)modal.lookup("#botDifficulty"+difficulty.name());
                    check(button!=null,"difficulty button exists: "+difficulty);
                    button.fire();
                    check(socket.last!=null&&socket.last.getType()==MessageType.CREATE_BOT_MATCH
                        &&((BotMatchRequest)socket.last.getPayload()).difficulty()==difficulty,"click starts chosen difficulty: "+difficulty);
                    check(Main.notifications().root().lookup("#customDialog")==null,"selection closes popup");
                }
                Main.historyMatchId="match";FXMLLoader detailLoader=new FXMLLoader(Main.class.getResource("/views/match_detail.fxml"));
                Parent detailRoot=detailLoader.load();MatchDetailController detail=detailLoader.getController();Main.notifications().setPage(detailRoot);
                Match match=new Match("match","PVP",1,"A very long player name",2L,"Bob",1L,"HUMAN","ZERO_SCORE","2026-01-01",0,63,null,false,false);
                ArrayList<Dart> darts=new ArrayList<>();
                darts.add(new Dart(1,1,1L,"HUMAN",50,251,false));
                darts.add(new Dart(1,2,1L,"HUMAN",39,251,true));
                darts.add(new Dart(2,1,2L,"HUMAN",10,291,false));
                darts.add(new Dart(3,1,1L,"HUMAN",13,238,false));
                detail.updateDetail(new Detail(match,darts,null));
                for(int width:new int[]{1040,1600}){
                    Main.notifications().root().resize(width,780);Main.notifications().root().applyCss();Main.notifications().root().layout();
                    GridPane grid=(GridPane)detailRoot.lookup("#matchHistoryGrid");
                    check(grid!=null,"history grid exists");
                    for(int row=1;row<=2;row++){
                        final int rowIndex=row;
                        var cells=grid.getChildren().stream().filter(n->GridPane.getRowIndex(n)!=null&&GridPane.getRowIndex(n)==rowIndex).toList();
                        check(cells.size()==2&&Math.abs(cells.get(0).getBoundsInParent().getWidth()-cells.get(1).getBoundsInParent().getWidth())<1.1,"equal columns at width "+width+" row "+row);
                    }
                }
                System.out.println("ALL MATCH LAYOUT CHECKS PASSED");
            }catch(Throwable error){failure.set(error);}finally{socket.shutdown();done.countDown();}
        });
        if(!done.await(30,TimeUnit.SECONDS))throw new AssertionError("UI timeout");
        Platform.exit();if(failure.get()!=null)throw new AssertionError(failure.get());
    }
}
