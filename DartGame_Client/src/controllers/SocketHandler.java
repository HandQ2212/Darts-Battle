/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controllers;

import btl_ltm_n3.Main;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.User;

/**
 *
 * @author kaita
 */
public class SocketHandler {
    Socket s;
    DataInputStream dis;
    DataOutputStream dos;

    public String loginUser = null; // lưu tài khoản đăng nhập hiện tại
    Thread listener = null;
    public String competitor = "";
    String roomIdPresent = null; // lưu room hiện tại mà người chơi đang ở 
    public boolean checkYouAreInvited = false;
    
    public String connect(String addr, int port){
        try {
            s = new Socket(addr, port);
            
            // obtaining input and output streams
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());

            // close old listener
            if (listener != null && listener.isAlive()) {
                listener.interrupt();
            }

            // listen to server
            listener = new Thread(this::listen);
            listener.start();

            // connect success
            return "success";
        } catch (IOException ex) {
             return "failed;" + ex.getMessage();
        }
    }
    
    private void listen(){
        boolean running = true;
        while(running){
            try {
                // Nhận Dữ liệu - Phản hồi - Request từ Server
                String received = dis.readUTF();
                System.out.println("YOU: ("+ loginUser +") RECEIVED: " + received);

                String type = received.split(";")[0];
                switch (type) {
                    case "LOGIN":
                        onReceiveLogin(received);
                        break;
                    case "REGISTER":
                        onReceiveRegister(received);
                        break;
                    case "LOGOUT":
                        onReceiveLogout(received);
                        break;
                    case "GET_LIST_ONLINE":
                        onReceiveGetListOnline(received);
                        break;
                    case "GET_LEADERBOARD":
                        onReceiveGetLeaderboard(received);
                        break;
                    case "GET_USER_RANK":
                        onReceiveGetUserRank(received);
                        break;
                    case "GET_USER_STATS":
                        onReceiveGetUserStats(received);
                        break;
                    case "INVITE_TO_PLAY":
                        onReceiveInviteToPlay(received);
                        break;
                    case "ACCEPT_PLAY":
                        onReceiveAcceptPlay(received);
                        break;
                    case "NOT_ACCEPT_PLAY":
                        onReceiveNotAcceptPlay(received);
                        break;
                    case "LEAVE_TO_GAME":
                        onReceiveLeaveToGame(received);
                        break; 
                    case "TURN_THROW":
                        onReceiveTurnThrow(received);
                        break;
                    case "TURN_ROTATE":
                        onReceiveTurnRotate(received);
                        break;
                    case "GET_MATCH_HISTORY":
                        onReceiveGetMatchHistory(received);
                        break;
                    case "CHAT_MESSAGE": // Ng gửi, ng nhận, roomId,  message
                        onReceiveChatMessage(received);
                        break; 
                    case "END_GAME":
                        onReceiveEndGame(received);
                        break;
                    case "EXIT":
                        running = false;
                        break;
                }
            } catch (IOException ex) {
                Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
                running = false;    
            }
        }
    }
    
    
    // ------------------------------------------------------------------------
    // RECEIVE: 
    private void onReceiveLogin(String received) {
    // get status from data
        String[] splitted = received.split(";");
        String status = splitted[1];

        Platform.runLater(() -> {
            if (status.equals("failed")) {
                // hiển thị lỗi
                String failedMsg = splitted.length > 2 ? splitted[2] : "Tên đăng nhập hoặc mật khẩu không đúng.";
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Đăng nhập thất bại");
                alert.setHeaderText(null);
                alert.setContentText(failedMsg);
                alert.showAndWait();
            } else if (status.equals("success")) {
                // lưu user login
                this.loginUser = splitted[2];

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Đăng nhập thành công");
                alert.setHeaderText(null);
                alert.setContentText("Chào mừng " + loginUser + " quay lại!");
                alert.showAndWait();

                try {
                    Main.setRoot("home");
                } catch (Exception ex) {
                    Alert alertError = new Alert(Alert.AlertType.ERROR);
                    alertError.setTitle("Lỗi giao diện");
                    alertError.setHeaderText(null);
                    alertError.setContentText("Không tìm thấy trang Home. Vui lòng kiểm tra lại.");
                    alertError.showAndWait();
                }
            }
        });
    }

    
    private void onReceiveRegister(String received) {
        // get status from data
        String[] splitted = received.split(";");
        String status = splitted[1];

        if (status.equals("failed")) {
            // hiển thị lỗi bằng Alert
            String failedMsg = splitted.length > 2 ? splitted[2] : "Lỗi không xác định.";

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Đăng ký thất bại");
                alert.setHeaderText("Không thể đăng ký tài khoản");
                alert.setContentText("Vui lòng kiểm tra lại: " + failedMsg);
                alert.showAndWait();
            });

        } else if (status.equals("success")) {
            Platform.runLater(() -> {
                try {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thành công");
                    alert.setHeaderText("Đăng ký thành công!");
                    alert.setContentText("Bạn sẽ được chuyển về trang đăng nhập.");
                    alert.showAndWait();

                    Main.setRoot("login");
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Lỗi hệ thống");
                    alert.setHeaderText("Không tìm thấy trang Login");
                    alert.setContentText("Vui lòng kiểm tra lại đường dẫn hoặc liên hệ quản trị viên.");
                    alert.showAndWait();
                }
            });
        }
    }

    
    private void onReceiveLogout(String received) {
        // get status from data
        String[] splitted = received.split(";");
        String status = splitted[1];
        
        if (status.equals("success")) {
            try {
                Main.setRoot("login");
            } catch (Exception ex) {
                System.out.println("Trang không tồn tại, kiểm tra lại Router App"); // Cần UI: Trang Notfound
            }
        }
    }
    private void onReceiveGetListOnline(String received) {
        // get status from data
        String[] splitted = received.split(";");
        String status = splitted[1];

        if (status.equals("success")) {
            int userCount = Integer.parseInt(splitted[2]);

            // Reset lại list
            Main.listOnlineUser.clear();

            if (userCount > 0) {
                for (int i = 3; i < userCount + 3; i++) {
                    String username = splitted[i];
                    if (!username.equals(loginUser) && !username.equals("null")) { // Ko tính người dùng đang online
                        // chỉ lưu username, id và status tạm thời để mặc định
                        User u = new User(i - 2, username, "online");
                        Main.listOnlineUser.add(u);
                    }
                }
            }

            // debug
            System.out.print("LIST USER ONLINE: ");
            for (User u : Main.listOnlineUser) {
                System.out.print(u.getUsername() + " ");
            }
            System.out.println();
            
            // Cập nhật bảng nếu controller đã load
            if (Main.chooseOpponentController != null) {
                Main.chooseOpponentController.updateUserTable(Main.listOnlineUser);
            }

        } else {
            System.out.println("Have some error!");
        }
    }
        
        //-- Invite Room + Accept + Reject
    private void onReceiveInviteToPlay(String received) {
        System.out.println("==================================");
        System.out.println("------INVITATION: " + received);

        String[] splitted = received.split(";");
        String status = splitted[1];
        
        if(status.equals("success")){
            String userHost = splitted[2];
            String userInvited = splitted[3];
            String roomId = splitted[4];
            System.out.println("Nhận được lời mời: "+ received);
            
            // Tạo Dialog mời người chơi vào trận (chắc để 1 cái log ở góc dưới thôi)
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NotificationDialog.fxml"));
                    Parent root = loader.load();

                    NotificationController controller = loader.getController();
                    controller.setMessage("Người chơi " + userHost + " mời bạn vào phòng " + roomId);
                    controller.setOnAccept(() -> {
                        System.out.println("Đồng ý tham gia phòng " + roomId);
                        try {
                            // TODO: gửi gói tin Accept lên server + Lưu Host + Invited User + Room ID vào
                            roomIdPresent = roomId;
                            this.competitor = userHost;
                            sendData("ACCEPT_PLAY;" + userHost + ";" + userInvited + ";" + roomId);
                            checkYouAreInvited = true;
                            Main.setRoot("startgame");
                        } catch (IOException ex) {
                            sendData("NOT_ACCEPT_PLAY;" + userHost + ";" + userInvited + ";" + roomId);
                            Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
                    controller.setOnDecline(() -> {
                        System.out.println("Từ chối tham gia phòng " + roomId);
                        // TODO: gửi gói tin Decline lên server
                    });

                    Stage dialogStage = new Stage();
                    dialogStage.initStyle(StageStyle.UNDECORATED);
                    dialogStage.setAlwaysOnTop(true);

                    Scene scene = new Scene(root);
                    dialogStage.setScene(scene);

                    // hiển thị ở góc dưới phải màn hình
                    Screen screen = Screen.getPrimary();
                    javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
                    dialogStage.setX(bounds.getMaxX() - 320); // 300 width + margin
                    dialogStage.setY(bounds.getMaxY() - 160); // 120 height + margin

                    dialogStage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        System.out.println("==================================");
    }
    private void onReceiveAcceptPlay(String received){
        String[] splitted = received.split(";");
        String status = splitted[1];
        
        if(status.equals("success")){
            String userHost = splitted[2];
            String userInvited = splitted[3];
            roomIdPresent = splitted[4];
            // Nhớ lưu dữ liệu của User Invited
            try {
                this.competitor = userInvited;
                System.out.println("Người chơi: " + userInvited + " đồng ý lời mời của bạn!");
                Main.setRoot("startgame");
            } catch (IOException ex) {
                Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private void onReceiveNotAcceptPlay(String received){
        String[] splitted = received.split(";");
        String status = splitted[1];
        
        if(status.equals("success")){
            String userHost = splitted[2];
            String userInvited = splitted[3];
            System.out.println("Người chơi: " + userInvited + " từ chối lời mời của bạn!");
        }
    }
    private void onReceiveLeaveToGame(String received) {
        String[] splitted = received.split(";");
        String status = splitted[1];
        
        if(status.equals("success")){
            String userHost = splitted[2];
            String userInvited = splitted[3];
            Platform.runLater(() -> {
                if (Main.startGameController != null) {
                    Main.startGameController.showWinnerDialog(userHost);
                }
            });
        }
    }
    private void onReceiveTurnThrow(String received){
        String[] splitted = received.split(";");
        String userTurn = splitted[1];
        
        if(splitted.length == 9){
            String competitorName = splitted[2];
            String roomId = splitted[3];
            String aigle = splitted[4];
            String score1 = splitted[5];
            String score2 = splitted[6];
            String score3 = splitted[7];
            String scoreRemaining = splitted[8];
            
            Platform.runLater(() -> {
                Main.startGameController.updateCompetitorStatus(competitorName, roomId,aigle, score1, score2, score3, scoreRemaining);
            });
        }
        Platform.runLater(() -> {
            Main.startGameController.setTurn(userTurn);
        });
    }
    private void onReceiveTurnRotate(String received){
        String[] splitted = received.split(";");
        String userTurn = splitted[1];
        Platform.runLater(() -> {
            Main.startGameController.setTurnRotate(userTurn);
        });
    }
    private void onReceiveEndGame(String received){
        String[] splitted = received.split(";");
        String userName = splitted[1];
        String competitorName = splitted[2];
        String roomId = splitted[3];
        String winnerName = splitted[4];
        System.out.println("Trận đấu kết thúc, người chiến thắng là: " + winnerName);
        
         // Gọi UI hiển thị thông báo trên giao diện game
        Platform.runLater(() -> {
            if (Main.startGameController != null) {
                Main.startGameController.showWinnerDialogEndGame(winnerName);
            } else {
                System.out.println("Không tìm thấy StartGameController để hiển thị kết quả!");
            }
        });
    }
    public void onReceiveChatMessage(String received){
        String[] splitted = received.split(";");
        String sendName = splitted[1];
        String competitorName = splitted[2];
        String roomId = splitted[3];
        String message = splitted[4];
                
        // set chat vào giao diện (check đúng room - compe tránh lỗi)
        if(sendName.equals(competitor) && roomId.equals(roomIdPresent) && Main.startGameController != null){
            Platform.runLater(() -> {
                Main.startGameController.addMessage(sendName + ": " + message);
            });
        }
        else System.out.println("Cõ lối xảy ra");
    }
    
    /**
     * Nhận dữ liệu bảng xếp hạng từ server
     * Format: GET_LEADERBOARD;success;count;player1Data;player2Data;...
     * playerData: userId|username|score
     */
    private void onReceiveGetLeaderboard(String received) {
        String[] splitted = received.split(";");
        String status = splitted[1];
        
        if (status.equals("success")) {
            int count = Integer.parseInt(splitted[2]);
            
            // Xóa dữ liệu cũ
            Main.leaderboardData.clear();
            
            // Parse dữ liệu từng player: userId|username|score
            for (int i = 0; i < count; i++) {
                String playerData = splitted[3 + i];
                Main.leaderboardData.add(playerData);
            }
            
            System.out.println("Received leaderboard data: " + count + " players");
            
            // Cập nhật UI nếu RankingController đã được khởi tạo
            Platform.runLater(() -> {
                if (Main.rankingController != null) {
                    Main.rankingController.updateLeaderboardTable();
                }
            });
            
        } else {
            String errorMsg = splitted.length > 2 ? splitted[2] : "Unknown error";
            System.err.println("Failed to get leaderboard: " + errorMsg);
            
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setHeaderText("Không thể tải bảng xếp hạng");
                alert.setContentText(errorMsg);
                alert.showAndWait();
            });
        }
    }
    
    /**
     * Nhận thứ hạng của user từ server
     * Format: GET_USER_RANK;success;rank hoặc GET_USER_RANK;failed;error
     */
    private void onReceiveGetUserRank(String received) {
        String[] splitted = received.split(";");
        String status = splitted[1];
        
        if (status.equals("success")) {
            int rank = Integer.parseInt(splitted[2]);
            System.out.println("Your rank: " + rank);
            
            // Cập nhật UI
            Platform.runLater(() -> {
                if (Main.rankingController != null) {
                    Main.rankingController.updateUserRank(rank);
                }
            });
            
        } else {
            String errorMsg = splitted.length > 2 ? splitted[2] : "Unknown error";
            System.err.println("Failed to get user rank: " + errorMsg);
        }
    }
    
    /**
     * Nhận thống kê của user từ server
     * Format đơn giản: GET_USER_STATS;success;username|score
     */
    private void onReceiveGetUserStats(String received) {
        String[] splitted = received.split(";");
        String status = splitted[1];
        
        if (status.equals("success")) {
            String statsData = splitted[2];
            String[] stats = statsData.split("\\|");
            
            System.out.println("User stats received:");
            System.out.println("  Username: " + stats[0]);
            System.out.println("  Score: " + stats[1]);
            
            // Cập nhật UI
            Platform.runLater(() -> {
                if (Main.rankingController != null) {
                    Main.rankingController.updateUserStats(statsData);
                }
            });
            
        } else {
            String errorMsg = splitted.length > 2 ? splitted[2] : "Unknown error";
            System.err.println("Failed to get user stats: " + errorMsg);
        }
    }
    private void onReceiveGetMatchHistory(String received){
        // Format server gửi:
        // GET_MATCH_HISTORY;success;count;match1;match2;match3;...

        String[] splitted = received.split(";");
        String status = splitted[1];

        if (!status.equals("success")) {
            System.out.println("Lấy lịch sử đấu thất bại!");
            return;
        }

        int count = Integer.parseInt(splitted[2]);

        List<String> matchList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            matchList.add(splitted[3 + i]);
        }

        // Gọi controller để cập nhật bảng
        Platform.runLater(() -> {
            if (Main.matchHistoryController != null) {
                Main.matchHistoryController.updateMatchHistory(matchList);
            }
        });
    }
    // ------------------------------------------------------------------------
    // SEND:
    public void sendData(String data) {
        try {
            System.out.println("Data Client Sended: "+ data);
            dos.writeUTF(data);
        } catch (IOException ex) {
            Logger.getLogger(SocketHandler.class
                .getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public void login(String email, String password) {
        // prepare data
        String data = "LOGIN" + ";" + email + ";" + password;
        // send data
        sendData(data);
    }
    public void register(String email, String password) {
        // prepare data
        String data = "REGISTER" + ";" + email + ";" + password;
        // send data
        sendData(data);
    }
    public void logout() {
        // prepare data
        this.loginUser = null;
        sendData("LOGOUT");
    }
    public void getListOnline() {
        sendData("GET_LIST_ONLINE");
    }
    public void checkStatusUser(String username){
        sendData("CHECK_sTATUS_USER;" + username);
    }
    public void inviteToPlay(String opponentName){
        sendData("INVITE_TO_PLAY;" + loginUser + ";" + opponentName);
    }
    public void leaveGame(){
        sendData("LEAVE_TO_GAME;" + loginUser + ";" + competitor + ";" + roomIdPresent);
    }
    
    /**
     * Gửi request lấy bảng xếp hạng
     * @param limit Số lượng người chơi muốn lấy (mặc định 100)
     */
    public void getLeaderboard(int limit) {
        sendData("GET_LEADERBOARD;" + limit);
    }
    
    /**
     * Gửi request lấy thứ hạng của user
     * @param username Tên người chơi
     */
    public void getUserRank(String username) {
        sendData("GET_USER_RANK;" + username);
    }
    
    /**
     * Gửi request lấy thống kê của user
     * @param username Tên người chơi
     */
    public void getUserStats(String username) {
        sendData("GET_USER_STATS;" + username);
    }
    public void getMatchHistory(String username) {
        sendData("GET_MATCH_HISTORY;" + username);
    }
    
    // Getter setter:

    public String getRoomIdPresent() {
        return roomIdPresent;
    }

    public void setRoomIdPresent(String roomIdPresent) {
        this.roomIdPresent = roomIdPresent;
    }
    
}
