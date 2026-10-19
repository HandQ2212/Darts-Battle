package controllers;

import btl_ltm_n3.Main;
import enums.MatchMode;
import enums.PlayerStatus;
import models.User;
import protocol.Message;
import protocol.MessageType;
import protocol.dto.LoginRequest;
import protocol.dto.ThrowCoordinateRequest;
import protocol.dto.ThrowResolvedResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SocketHandler {
    private Socket s;
    private ObjectInputStream dis;
    private ObjectOutputStream dos;

    public String loginUser = null; 
    public long loggedInUserId = -1;
    private Thread listener = null;
    
    public String competitor = "";
    public String roomIdPresent = null;
    public boolean checkYouAreInvited = false;
    
    public String connect(String addr, int port) {
        try {
            s = new Socket(addr, port);
            dos = new ObjectOutputStream(s.getOutputStream());
            dis = new ObjectInputStream(s.getInputStream());

            if (listener != null && listener.isAlive()) {
                listener.interrupt();
            }

            listener = new Thread(this::listen);
            listener.start();

            return "success";
        } catch (Exception ex) {
             return "failed;" + ex.getMessage();
        }
    }
    
    private void listen() {
        boolean running = true;
        while (running) {
            try {
                Message message = (Message) dis.readObject();
                System.out.println("YOU: ("+ loginUser +") RECEIVED: " + message.getType());

                switch (message.getType()) {
                    case LOGIN_RESULT:
                        onReceiveLogin(message);
                        break;
                    case REGISTER_RESULT:
                        onReceiveRegister(message);
                        break;
                    case LOGOUT_RESULT:
                        onReceiveLogout(message);
                        break;
                    case ONLINE_LIST:
                        // Server to implement broadcasting
                        break;
                    case INVITE_RECEIVED:
                        onReceiveInviteToPlay(message);
                        break;
                    case INVITE_RESULT:
                        // onReceiveAcceptPlay / Not Accept
                        break;
                    case MATCH_STARTED:
                        onReceiveMatchStarted(message);
                        break;
                    case TURN_PREPARING:
                        onReceiveTurnPreparing(message);
                        break;
                    case ROTATION_APPLIED:
                        onReceiveRotationApplied(message);
                        break;
                    case TURN_STARTED:
                        onReceiveTurnStarted(message);
                        break;
                    case THROW_RESOLVED:
                        onReceiveThrowResolved(message);
                        break;
                    case MATCH_FINISHED:
                        onReceiveEndGame(message);
                        break;
                    case CHAT_MESSAGE:
                        onReceiveChatMessage(message);
                        break;
                    case RANKING_RESULT:
                        onReceiveGetLeaderboard(message);
                        break;
                    case HISTORY_RESULT:
                        onReceiveGetMatchHistory(message);
                        break;
                    case ERROR:
                        onReceiveError(message);
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                running = false;    
            }
        }
    }
    
    // ------------------------------------------------------------------------
    // RECEIVE METHODS

    private void onReceiveLogin(Message msg) {
        String status = (String) msg.getPayload();
        Platform.runLater(() -> {
            if ("SUCCESS".equals(status)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Đăng nhập thành công");
                alert.setHeaderText(null);
                alert.setContentText("Chào mừng " + loginUser + " quay lại!");
                alert.showAndWait();
                try { Main.setRoot("home"); } catch (Exception ex) {}
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Đăng nhập thất bại");
                alert.setHeaderText(null);
                alert.setContentText("Sai thông tin.");
                alert.showAndWait();
            }
        });
    }

    private void onReceiveRegister(Message msg) {
        // Implement
    }

    private void onReceiveLogout(Message msg) {
        String status = (String) msg.getPayload();
        if ("SUCCESS".equals(status)) {
            Platform.runLater(() -> {
                try { Main.setRoot("login"); } catch (Exception ex) {}
            });
        }
    }

    private void onReceiveInviteToPlay(Message msg) {
        // Implement
    }

    private void onReceiveMatchStarted(Message msg) {
        roomIdPresent = (String) msg.getPayload();
        Platform.runLater(() -> {
            try {
                // Should init GameViewState here
                Main.gameViewState.startMatch(roomIdPresent, MatchMode.PVP, loginUser, competitor);
                Main.setRoot("startgame");
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    private void onReceiveTurnPreparing(Message msg) {
        long turnId = (Long) msg.getPayload();
        Platform.runLater(() -> {
            Main.gameViewState.startTurn(turnId);
            if (Main.startGameController != null) {
                Main.startGameController.setTurnRotate(Main.gameViewState.getCurrentActorName());
            }
        });
    }

    private void onReceiveRotationApplied(Message msg) {
        int degree = (Integer) msg.getPayload();
        Platform.runLater(() -> {
            Main.gameViewState.setRotationDegree(degree);
            if (Main.startGameController != null) {
                Main.startGameController.applyBoardRotationLocally(degree);
            }
        });
    }
    
    private void onReceiveTurnStarted(Message msg) {
        Platform.runLater(() -> {
            if (Main.startGameController != null) {
                Main.startGameController.setTurn(Main.gameViewState.getCurrentActorName());
            }
        });
    }

    private void onReceiveThrowResolved(Message msg) {
        ThrowResolvedResponse response = (ThrowResolvedResponse) msg.getPayload();
        Platform.runLater(() -> {
            Main.gameViewState.applyThrow(response);
            if (Main.startGameController != null) {
                Main.startGameController.updateCompetitorStatus(response);
            }
        });
    }

    private void onReceiveEndGame(Message msg) {
        String winnerId = (String) msg.getPayload();
        Platform.runLater(() -> {
            if (Main.startGameController != null) {
                Main.startGameController.showWinnerDialogEndGame(winnerId);
            }
        });
    }

    private void onReceiveChatMessage(Message msg) {
        // Implement
    }

    private void onReceiveGetLeaderboard(Message msg) {
        // Implement
    }

    private void onReceiveGetMatchHistory(Message msg) {
        // Implement
    }

    private void onReceiveError(Message msg) {
        String errorMsg = (String) msg.getPayload();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText(errorMsg);
            alert.showAndWait();
        });
    }

    // ------------------------------------------------------------------------
    // SEND METHODS

    public void sendData(Message message) {
        try {
            System.out.println("Data Client Sended: " + message.getType());
            dos.writeObject(message);
            dos.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void login(String username, String password) {
        this.loginUser = username;
        sendData(Message.of(MessageType.LOGIN_REQUEST, new LoginRequest(username, password)));
    }

    public void register(String username, String password) {
        // Assuming RegisterRequest is same as LoginRequest for now or use Object[]
        sendData(Message.of(MessageType.REGISTER_REQUEST, new LoginRequest(username, password)));
    }

    public void logout() {
        this.loginUser = null;
        sendData(Message.of(MessageType.LOGOUT_REQUEST, null));
    }

    public void getListOnline() {
        sendData(Message.of(MessageType.ONLINE_LIST, null));
    }

    public void inviteToPlay(String opponentName) {
        sendData(Message.of(MessageType.INVITE_REQUEST, opponentName));
    }

    public void leaveGame() {
        sendData(Message.of(MessageType.LEAVE_MATCH, roomIdPresent));
    }

    public void getLeaderboard(int limit) {
        sendData(Message.of(MessageType.GET_RANKING, limit));
    }

    public void getMatchHistory(String username) {
        sendData(Message.of(MessageType.GET_HISTORY, username));
    }
    
    public void createBotMatch() {
        sendData(Message.of(MessageType.CREATE_BOT_MATCH, null));
    }

    public void submitRotation(int degree) {
        sendData(Message.of(MessageType.ROTATION_SUBMIT, new Object[]{roomIdPresent, degree}));
    }

    public void submitThrowCoordinate(double x, double y) {
        sendData(Message.of(MessageType.THROW_COORDINATE, new ThrowCoordinateRequest(roomIdPresent, Main.gameViewState.getTurnId(), x, y)));
    }
}
