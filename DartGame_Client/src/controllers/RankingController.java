package controllers;
import btl_ltm_n3.Main;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import protocol.dto.HistoryMessages.*;
public class RankingController {
    @FXML private Label userRankLabel,userStatsLabel;
    @FXML private HBox loadingBox;
    @FXML private TableView<Rank> leaderboardTable;
    @FXML private TableColumn<Rank,Integer> rankColumn,scoreColumn;
    @FXML private TableColumn<Rank,String> usernameColumn;
    @FXML private TableColumn<Rank,Void> detailsColumn;
    @FXML public void initialize(){
        Main.rankingController=this;leaderboardTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        rankColumn.setCellValueFactory(c->new SimpleIntegerProperty(c.getValue().rank()).asObject());
        scoreColumn.setCellValueFactory(c->new SimpleIntegerProperty(c.getValue().wins()).asObject());
        usernameColumn.setCellValueFactory(c->new SimpleStringProperty(c.getValue().name()));
        detailsColumn.setCellFactory(col->new TableCell<>(){
            private final Button button=new Button("Chi tiết");
            {button.setOnAction(e->{Rank row=getTableRow().getItem();if(row==null)return;
                Main.historyPlayerId=row.userId();Main.historyPlayerName=row.name();Main.historyPage=0;Main.historyOrigin="ranking";
                try{Main.setRoot("match_history");}catch(Exception x){Main.socketHandler.showError("Không mở được lịch sử.");}});
            }
            @Override protected void updateItem(Void value,boolean empty){super.updateItem(value,empty);setGraphic(empty?null:button);}
        });
        loadLeaderboardData();
    }
    public void loadLeaderboardData(){loadingBox.setVisible(true);loadingBox.setManaged(true);Main.socketHandler.getRankingsV2();}
    public void updateRankings(Rankings result){
        loadingBox.setVisible(false);loadingBox.setManaged(false);
        if(result==null||result.error()!=null){Main.notifications().toast("Bảng xếp hạng",result==null?"Không tải được dữ liệu.":result.error(),"error");return;}
        leaderboardTable.setItems(FXCollections.observableArrayList(result.rows()));
        result.rows().stream().filter(r->r.userId()==Main.socketHandler.loggedInUserId).findFirst().ifPresent(r->{userRankLabel.setText("Hạng của bạn: #"+r.rank());userStatsLabel.setText("Số trận thắng: "+r.wins());});
    }
    public void updateLeaderboardTable(){}
    @FXML public void handleRefresh(){loadLeaderboardData();}
    @FXML public void handleBack(){try{Main.setRoot("home");}catch(Exception e){Main.socketHandler.showError("Không về được sảnh.");}}
}
