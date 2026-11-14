package controllers;
import btl_ltm_n3.Main;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import protocol.dto.HistoryMessages.*;
import java.util.List;
public class MatchHistoryController {
    @FXML private Label headingLabel,pageLabel;
    @FXML private TableView<Match> historyTable;
    @FXML private TableColumn<Match,String> usernameCol,opponentCol,resultCol,timeCol;
    @FXML private TableColumn<Match,Void> detailsCol;
    @FXML private HBox loadingBox;
    @FXML private Button previousButton,nextButton;
    @FXML public void initialize(){
        Main.matchHistoryController=this;headingLabel.setText("Lịch sử của "+Main.historyPlayerName);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        usernameCol.setCellValueFactory(c->new SimpleStringProperty(Main.historyPlayerName));
        opponentCol.setCellValueFactory(c->{Match m=c.getValue();return new SimpleStringProperty(m.player1Id()==Main.historyPlayerId?(m.player2Name()==null?"BOT":m.player2Name()):m.player1Name());});
        resultCol.setCellValueFactory(c->new SimpleStringProperty(java.util.Objects.equals(c.getValue().winnerId(),Main.historyPlayerId)?"Thắng":"Thua"));
        timeCol.setCellValueFactory(c->new SimpleStringProperty(c.getValue().endedAt()));
        detailsCol.setCellFactory(col->new TableCell<>(){
            private final Button button=new Button("Chi tiết");
            {button.setOnAction(e->{Match m=getTableRow().getItem();if(m!=null){Main.historyMatchId=m.matchId();try{Main.setRoot("match_detail");}catch(Exception x){Main.socketHandler.showError("Không mở được chi tiết.");}}});}
            @Override protected void updateItem(Void value,boolean empty){super.updateItem(value,empty);setGraphic(empty?null:button);}
        });loadHistory();
    }
    public void loadHistory(){loadingBox.setVisible(true);loadingBox.setManaged(true);previousButton.setDisable(true);nextButton.setDisable(true);Main.socketHandler.getHistoryPage(Main.historyPlayerId,Main.historyPage);}
    public void updatePage(Page page){
        if(page==null||page.playerId()!=Main.historyPlayerId||page.page()!=Main.historyPage)return;
        loadingBox.setVisible(false);loadingBox.setManaged(false);previousButton.setDisable(Main.historyPage==0);nextButton.setDisable(!page.hasNext());pageLabel.setText("Trang "+(Main.historyPage+1));
        if(page.error()!=null){Main.notifications().toast("Lịch sử",page.error(),"error");return;}
        historyTable.setItems(FXCollections.observableArrayList(page.matches()));
    }
    public void updateMatchHistory(List<String> legacy){}
    @FXML public void onReloadClick(){loadHistory();}
    @FXML public void previous(){if(Main.historyPage>0){Main.historyPage--;loadHistory();}}
    @FXML public void next(){Main.historyPage++;loadHistory();}
    @FXML public void handleBack(){try{Main.setRoot(Main.historyOrigin);}catch(Exception e){Main.socketHandler.showError("Không quay lại được.");}}
}
