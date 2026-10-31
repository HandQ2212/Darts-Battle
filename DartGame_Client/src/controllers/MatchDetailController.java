package controllers;
import btl_ltm_n3.Main;
import enums.BotDifficulty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import protocol.dto.HistoryMessages.*;
import java.util.*;

public class MatchDetailController {
    @FXML private Label summaryLabel,statusLabel,headingLabel;
    @FXML private VBox historyRows;
    @FXML public void initialize(){Main.matchDetailController=this;Main.socketHandler.getMatchDetail(Main.historyMatchId);}
    public void updateDetail(Detail detail){
        if(detail==null||detail.error()!=null){statusLabel.setText(detail==null?"Không tải được dữ liệu.":detail.error());return;}
        Match m=detail.match();if(m==null||!Objects.equals(m.matchId(),Main.historyMatchId))return;
        String p2=m.player2Name()==null?"BOT":m.player2Name(),winner=m.winnerId()==null?("BOT".equals(m.winnerType())?"BOT":"Chưa rõ"):Objects.equals(m.winnerId(),m.player1Id())?m.player1Name():p2;
        String difficulty="";if(m.difficulty()!=null)try{difficulty=" · "+BotDifficulty.valueOf(m.difficulty()).label();}catch(Exception ignored){}
        headingLabel.setText(m.player1Name()+"  —  "+p2);
        summaryLabel.setText(("PVE".equals(m.mode())?"Đấu bot":"PvP")+difficulty+"  ·  "+m.endedAt()+"\nNgười thắng: "+winner+"  ·  Điểm cuối: "+score(m.score1())+" — "+score(m.score2())+"\nLý do: "+reason(m.reason()));
        statusLabel.setText(m.incomplete()?"Lịch sử cũ thiếu thông tin; một số dữ liệu không xác định.":m.reconstructed()?"Một số dữ liệu cũ được dựng lại từ phi tiêu đã lưu.":"");
        historyRows.getChildren().clear();
        LinkedHashMap<Long,List<Dart>> p1=new LinkedHashMap<>(),other=new LinkedHashMap<>();
        for(Dart d:detail.darts()){
            boolean own=Objects.equals(d.playerId(),m.player1Id());
            (own?p1:other).computeIfAbsent(d.turn(),key->new ArrayList<>()).add(d);
        }
        var a=new ArrayList<>(p1.values());var b=new ArrayList<>(other.values());
        if(a.isEmpty()&&b.isEmpty()){historyRows.getChildren().add(new Label("Không có dữ liệu phi tiêu."));return;}
        Label left=new Label(m.player1Name()),right=new Label(p2);left.getStyleClass().add("history-heading");right.getStyleClass().add("history-heading");
        GridPane grid=new GridPane();grid.setId("matchHistoryGrid");grid.setHgap(10);grid.setVgap(8);
        for(int col=0;col<2;col++){ColumnConstraints constraint=new ColumnConstraints();constraint.setPercentWidth(50);constraint.setHgrow(Priority.ALWAYS);constraint.setMinWidth(0);grid.getColumnConstraints().add(constraint);}
        grid.add(left,0,0);grid.add(right,1,0);
        for(int i=0;i<Math.max(a.size(),b.size());i++){
            grid.add(cell(i<a.size()?a.get(i):null,i+1),0,i+1);grid.add(cell(i<b.size()?b.get(i):null,i+1),1,i+1);
        }
        historyRows.getChildren().add(grid);
    }
    private VBox cell(List<Dart> turn,int number){
        String[] values={"—","—","—"};int total=0;Integer remaining=null;boolean known=true;
        if(turn!=null)for(Dart d:turn){if(d.index()<1||d.index()>3)continue;values[d.index()-1]=d.score()+(d.bust()==null?" (?)":d.bust()?" BUST":"");if(d.bust()==null)known=false;else if(!d.bust())total+=d.score();remaining=d.remaining();}
        String summary=turn==null?"Chưa có lượt":(known?"Trừ "+total:"Trừ ?")+"  ·  Còn "+score(remaining);
        return ui.TurnHistoryView.cell(number,String.join("   ·   ",values),summary);
    }
    private String score(Integer n){return n==null?"?":n.toString();}
    private String reason(String value){if(value==null)return "Không rõ";return switch(value){case "FORFEIT"->"Bỏ cuộc";case "DISCONNECT"->"Mất kết nối";case "TURN_TIMEOUT"->"Hết thời gian";case "LOGOUT"->"Đăng xuất";default->"Về 0 điểm";};}
    @FXML private void handleBack(){try{Main.setRoot("match_history");}catch(Exception e){Main.socketHandler.showError("Không quay lại được.");}}
}
