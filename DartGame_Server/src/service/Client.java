/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import controller.LeaderboardController;
import controller.UserController;
import static dartgame_server.DartGame_Server.clientManager;
import static dartgame_server.DartGame_Server.isShutDown;
import static dartgame_server.DartGame_Server.roomManager;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author kaita
 */
public class Client implements Runnable{
    Socket s;
    DataInputStream dis;
    DataOutputStream dos;
    
    // Mỗi luồng là 1 Client => Mỗi luồng sẽ lưu các thông tin sau:
    Room joinedRoom; 
    String loginUser;
    Client cCompetitor;
    
    public Client(Socket s) throws IOException {
        this.s = s;
        // obtaining input and output streams 
        this.dis = new DataInputStream(s.getInputStream());
        this.dos = new DataOutputStream(s.getOutputStream());
    }
      
    @Override
    public void run() {
        String received;
        boolean running = true;

        while (!isShutDown) {
            try {
                // receive the request from client
                received = dis.readUTF();

                System.out.println("Data Server Received: " + received);
                String type = received.split(";")[0];
               
                switch (type) {
                    case "LOGIN":
                        onReceiveLogin(received);
                        break;
                    case "REGISTER":
                        onReceiveRegister(received);
                        break;
                    case "GET_LIST_ONLINE":
                        onReceiveGetListOnline();
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
                    case "GET_MATCH_HISTORY":
                        onReceiveGetMatchHistory(received);
                        break;
//                    case "GET_INFO_USER":
//                        onReceiveGetInfoUser(received);
//                        break;
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
                    case "THROW_RESULT":
                        onReceiveThrowResult(received);
                        break; 
                    case "ROTATE_RESULT":
                        onReceiveRotateResult(received);
                        break; 
                    case "CHAT_MESSAGE": // Ng gửi, ng nhận, roomId,  message
                        onReceiveChatMessage(received);
                        break; 
                    case "LOGOUT":
                        onReceiveLogout();
                        break;  
                    case "EXIT":
                        running = false;
                        break;
                }

            } catch (IOException ex) {
                break;
            } 
//            catch (SQLException ex) {
//                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
//            }
        }

        try {
            // closing resources 
            this.s.close();
            this.dis.close();
            this.dos.close();
            System.out.println("- Client disconnected: " + s);

            // remove from clientManager
            clientManager.remove(this);

        } catch (IOException ex) {
            System.out.println("ERROR RUNNING: "+ ex);
        }
    }
      
    

    // send data fucntions
    public String sendData(String data) {
        try {
            this.dos.writeUTF(data);
            return "success";
        } catch (IOException e) {
            System.err.println("Send data failed!");
            return "failed;" + e.getMessage();
        }
    }
    
    private void onReceiveLogin(String received) {
        // get email / password from data
        String[] splitted = received.split(";");
        String username = splitted[1];
        String password = splitted[2];
        String result = "";
        // Check xem ng dùng đã đăng nhập chưa:
        if(clientManager.find(username) != null){
            result = "failed;" + "Người dùng đẫ đăng nhập ở thiết bị khác. Vui lòng đăng xuất khởi các thiết bị trước khi đăng nhập lại";
            sendData("LOGIN" + ";" + result);
            return;
        }
        // check login
        result = new UserController().login(username, password);

        if (result.split(";")[0].equals("success")) {
            // set login user
            this.loginUser = username;
        }
        
        // send result
        sendData("LOGIN" + ";" + result);
        onReceiveGetListOnline();
    }
    private void onReceiveRegister(String received) {
        // get email / password from data
        String[] splitted = received.split(";");
        String username = splitted[1];
        String password = splitted[2];

        // reigster
        String result = new UserController().register(username, password);

        // send result
        sendData("REGISTER" + ";" + result);
    }
    private void onReceiveLogout() {
        this.loginUser = null;
        // send result
        sendData("LOGOUT" + ";" + "success");
        onReceiveGetListOnline();
    }
    private void onReceiveGetListOnline() {
        String result = clientManager.getListUseOnline();
        
        // send result
        String msg = "GET_LIST_ONLINE" + ";" + result;
        clientManager.broadcast(msg);
    }
    private void onReceiveInviteToPlay(String received){
        // Check Status trước đã
        System.out.println("==================================");
        System.out.println("Client Invited String: " + received);

        String[] splitted = received.split(";");
        String hostname = splitted[1];
        String invitedname = splitted[2]; // Check tk dc moi
        
        String status = "";
        Client c = clientManager.find(invitedname);
        System.out.println("------Client Invited: " + c.getLoginUser());
        if (c == null) {
            status = "OFFLINE";
        } else {
            if (c.getJoinedRoom() == null) {
                status = "ONLINE";

                joinedRoom = roomManager.createRoom();
                System.out.println("------Create new Room: " + joinedRoom.getId());

                joinedRoom.addClient(this);
                cCompetitor = clientManager.find(invitedname);
                
                // Send Invitation to Invited Ussr:
                String msg = "INVITE_TO_PLAY;" + "success;" + hostname + ";" + invitedname + ";" + joinedRoom.getId();
                System.out.println("------Message invite: " + msg);
                clientManager.sendToAClient(invitedname, msg);
            } else {
                status = "INGAME";
            }
        }
        System.out.println("==================================");
    }
    private void onReceiveCheckStatusUser(String received) {
        String[] splitted = received.split(";");
        String username = splitted[1];
        
        String status = "";
        Client c = clientManager.find(username);
        if (c == null) {
            status = "OFFLINE";
        } else {
            if (c.getJoinedRoom() == null) {
                status = "ONLINE";
            } else {
                status = "INGAME";
            }
        }
        // send result
        sendData("CHECK_STATUS_USER" + ";" + username + ";" + status);
    }
    private void onReceiveAcceptPlay(String received) {
        String[] splitted = received.split(";");
        String userHost = splitted[1];
        String userInvited = splitted[2];
        String roomId = splitted[3];
        
        Room room = roomManager.find(roomId);
        joinedRoom = room;
        joinedRoom.addClient(this);
        
        cCompetitor = clientManager.find(userHost);
        
        // Gửi kết quả là ng bên kia đồng ý vào cho Host + Gửi mes Turn_Throw cho Host (Mặc định Host đi trước)
        String msg = "ACCEPT_PLAY;" + "success;" + userHost + ";" + userInvited + ";" + joinedRoom.getId();
        clientManager.sendToAClient(userHost, msg);
        
        String turnMsg = "TURN_THROW;" + userHost;
        clientManager.sendToAClient(userHost, turnMsg); // Bắt Host đi trước
    }     
    private void onReceiveNotAcceptPlay(String received) {
        String[] splitted = received.split(";");
        String userHost = splitted[1];
        String userInvited = splitted[2];
        String roomId = splitted[3];
         
        // Xóa thông tin room khỏi client
        clientManager.find(userHost).setJoinedRoom(null);
        clientManager.find(userInvited).setJoinedRoom(null);
        
        // Xóa room: 
        Room room = roomManager.find(roomId);
        roomManager.remove(room);
        
        // send res: 
        String msg = "NOT_ACCEPT_PLAY;" + "success;" + userHost + ";" + userInvited + ";" + room.getId();
        clientManager.sendToAClient(userHost, msg);
    } 
    private void onReceiveLeaveToGame(String received) {
        // get email / password from data
        String[] splitted = received.split(";");
        String host = splitted[1];
        String invited = splitted[2];
        String roomId = splitted[3];

        joinedRoom.userLeaveGame(host);
        
        // Update điểm sau khi rời phòng
        String result = new UserController().increaseScore(invited);

        this.cCompetitor = null;
        this.joinedRoom = null;
        Room room = roomManager.find(roomId);
        roomManager.remove(room);
        
        // Sau khi xóa thông tin của người rời, xóa nốt room, competitor của ng còn lại đi
        Client c = clientManager.find(invited); 
        c.setJoinedRoom(null);
        c.setcCompetitor(null);
        
        // result: Gửi thông tin ng kia rời phòng cho ng còn lại 
        String msg = "LEAVE_TO_GAME;" + "success;" + host + ";" + invited;
        clientManager.sendToAClient(invited, msg);        
    }
    
    private String tmpPoint1, tmpPoint2, tmpPoint3, rmnPoint;
    private void onReceiveThrowResult(String received){ // THROW_RESULT;kaita123;competitorName;roomId;24;9;0;268
        String[] splitted = received.split(";");
        String userName = splitted[1];
        String competitorName = splitted[2];
        String roomId = splitted[3];
        String score1 = splitted[4];
        String score2 = splitted[5];
        String score3 = splitted[6];
        String scoreRemaining = splitted[7];
        tmpPoint1 = score1;
        tmpPoint2 = score2;
        tmpPoint3 = score3;
        rmnPoint = scoreRemaining;
        // B1: Check Winner: Nếu win: Gửi "END_GAME;winner" đến ng gửi + Xóa thông tin, Xóa phòng luôn đi
        if(Integer.parseInt(scoreRemaining) == 0){
            this.cCompetitor = null;
            this.joinedRoom = null;
            Room room = roomManager.find(roomId);
            roomManager.remove(room);

            // Sau khi xóa thông tin của người rời, xóa nốt room, competitor của ng còn lại đi
            Client c = clientManager.find(competitorName); 
            c.setJoinedRoom(null);
            c.setcCompetitor(null);

            Client c2 = clientManager.find(userName); 
            c2.setJoinedRoom(null);
            c2.setcCompetitor(null);
            
            String result = new UserController().increaseScore(userName);
            System.out.println("Result Update Score: "+ result);
            System.out.println("ALL ROOM: "+ roomManager.rooms);
            String endMsg = "END_GAME;" + userName + ";" + competitorName + ";" + roomId + ";" + userName;
            String endMsgCompetitor = "END_GAME;" + competitorName + ";" + userName + ";" + roomId + ";" + userName;
            // Lưu lịch sử đấu
            new UserController().saveMatchHistory(userName, competitorName, "WIN");
            new UserController().saveMatchHistory(competitorName, userName, "LOSE");

            clientManager.sendToAClient(userName, endMsg);
            clientManager.sendToAClient(competitorName, endMsgCompetitor);
        }
        // B2: Nếu chưa: Gửi "TURN_ROTATE" để ng kia xoay vòng quay: (Quay xong thì gửi lại server, để gửi hết thông tin cho ng còn lại)
        else{
            
            String turnMsg = "TURN_ROTATE;" + userName + ";" + competitorName + ";" + roomId;
            clientManager.sendToAClient(userName, turnMsg);
        }
    }
    private void onReceiveRotateResult(String received){ // ROTATE_RESULT;kaita123;competitorName;roomId;angle
        String[] splitted = received.split(";");
        String userName = splitted[1];
        String competitorName = splitted[2];
        String roomId = splitted[3];
        String angle = splitted[4];
        
        // Đổi lượt + Truyền thông tin về lượt của ng chơi này cho ng chơi kia
        String turnMsg = "TURN_THROW;" + competitorName + ";" + userName + ";" + roomId + ";" + angle + ";" + tmpPoint1 + ";" + tmpPoint2 + ";" + tmpPoint3 +";"+ rmnPoint;
        System.out.println("turnMsg: ------"+ turnMsg);
        clientManager.sendToAClient(competitorName, turnMsg);
    }
    
    public void onReceiveChatMessage(String received){
        String[] splitted = received.split(";");
        String userName = splitted[1];
        String competitorName = splitted[2];
        String roomId = splitted[3];
        String message = splitted[4];
        
        String data = "CHAT_MESSAGE" + ";" + userName + ";" + competitorName + ";" + roomId + ";" + message;
        
        // send data
        clientManager.sendToAClient(competitorName, data);
    }
    
    /**
     * Xử lý request lấy bảng xếp hạng
     * Format: GET_LEADERBOARD;limit hoặc GET_LEADERBOARD
     */
    private void onReceiveGetLeaderboard(String received) {
        LeaderboardController leaderboardController = new LeaderboardController();
        String result = leaderboardController.handleLeaderboardRequest(received);
        
        // Gửi kết quả về client
        sendData("GET_LEADERBOARD;" + result);
        System.out.println("✅ Sent leaderboard data to client: " + loginUser);
    }
    
    /**
     * Xử lý request lấy thứ hạng của user
     * Format: GET_USER_RANK;username
     */
    private void onReceiveGetUserRank(String received) {
        LeaderboardController leaderboardController = new LeaderboardController();
        String result = leaderboardController.handleUserRankRequest(received);
        
        // Gửi kết quả về client
        sendData("GET_USER_RANK;" + result);
        System.out.println("Sent user rank data to client: " + loginUser);
    }
    
    /**
     * Xử lý request lấy thống kê của user
     * Format: GET_USER_STATS;username
     */
    private void onReceiveGetUserStats(String received) {
        LeaderboardController leaderboardController = new LeaderboardController();
        String result = leaderboardController.handleUserStatsRequest(received);
        
        // Gửi kết quả về client
        sendData("GET_USER_STATS;" + result);
        System.out.println("Sent user stats data to client: " + loginUser);
    }
    private void onReceiveGetMatchHistory(String received){
         // received format: GET_MATCH_HISTORY;username
        String[] splitted = received.split(";");
        String username = splitted[1];

        // gọi controller
        String result = new UserController().getMatchHistory(username);

        // Gửi lại cho đúng client đã yêu cầu lịch sử
        clientManager.sendToAClient(username, "GET_MATCH_HISTORY;" + result);
    }
    
    
    
    // GET
    public Room getJoinedRoom(){ // TAm thoi de day da
        return null;
    }
    public String getLoginUser() {
        return loginUser;
    }
    public void setJoinedRoom(Room joinedRoom){ // TAm thoi de day da
        this.joinedRoom = joinedRoom;
    }

    public Client getcCompetitor() {
        return cCompetitor;
    }

    public void setcCompetitor(Client cCompetitor) {
        this.cCompetitor = cCompetitor;
    }

    
}




