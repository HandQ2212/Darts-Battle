import os
import shutil

# 1. Create DartGame_Common
common_files = {
    "DartGame_Common/src/protocol/Message.java": "package protocol;\n\npublic class Message {\n    private MessageType type;\n    private String requestId;\n    private Object payload;\n}\n",
    "DartGame_Common/src/protocol/MessageType.java": "package protocol;\n\npublic enum MessageType {\n    REGISTER_REQUEST, REGISTER_RESULT, LOGIN_REQUEST, LOGIN_RESULT, LOGOUT_REQUEST, LOGOUT_RESULT, ONLINE_LIST, INVITE_REQUEST, INVITE_RECEIVED, INVITE_ACCEPT, INVITE_REJECT, INVITE_RESULT, CREATE_BOT_MATCH, MATCH_STARTED, TURN_PREPARING, ROTATION_SUBMIT, ROTATION_APPLIED, TURN_STARTED, THROW_COORDINATE, THROW_RESOLVED, MATCH_FINISHED, CHAT_MESSAGE, LEAVE_MATCH, GET_RANKING, RANKING_RESULT, GET_HISTORY, HISTORY_RESULT, ERROR\n}\n",
    "DartGame_Common/src/protocol/dto/LoginRequest.java": "package protocol.dto;\n\npublic class LoginRequest {\n    private String username;\n    private String password;\n}\n",
    "DartGame_Common/src/protocol/dto/ThrowCoordinateRequest.java": "package protocol.dto;\n\npublic class ThrowCoordinateRequest {\n    private String matchId;\n    private long turnId;\n    private double x;\n    private double y;\n}\n",
    "DartGame_Common/src/protocol/dto/ThrowResolvedResponse.java": "package protocol.dto;\n\nimport enums.ActorType;\nimport enums.HitArea;\n\npublic class ThrowResolvedResponse {\n    private String matchId;\n    private long turnId;\n    private String actorName;\n    private ActorType actorType;\n    private int dartIndex;\n    private double x;\n    private double y;\n    private int rotationDegree;\n    private HitArea hitArea;\n    private int dartScore;\n    private int remainingScoreAfter;\n    private boolean isBust;\n    private boolean isTurnFinished;\n    private boolean isMatchFinished;\n}\n",
    "DartGame_Common/src/enums/PlayerStatus.java": "package enums;\n\npublic enum PlayerStatus {\n    OFFLINE, IDLE, INVITED, IN_MATCH\n}\n",
    "DartGame_Common/src/enums/MatchMode.java": "package enums;\n\npublic enum MatchMode {\n    PVP, PVE\n}\n",
    "DartGame_Common/src/enums/MatchStatus.java": "package enums;\n\npublic enum MatchStatus {\n    WAITING_ROTATION, PLAYING, FINISHED\n}\n",
    "DartGame_Common/src/enums/ActorType.java": "package enums;\n\npublic enum ActorType {\n    HUMAN, BOT\n}\n",
    "DartGame_Common/src/enums/HitArea.java": "package enums;\n\npublic enum HitArea {\n    MISS, SINGLE, DOUBLE, TRIPLE, OUTER_BULL, BULLSEYE\n}\n",
    "DartGame_Common/src/config/DartboardLayout.java": "package config;\n\npublic class DartboardLayout {\n    public static final int[] BOARD_ORDER = {20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5};\n}\n",
}

for filepath, content in common_files.items():
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    if not os.path.exists(filepath):
        with open(filepath, 'w') as f:
            f.write(content)

# 2. Rename directories in DartGame_Server
if os.path.exists("DartGame_Server/src/controller"):
    os.rename("DartGame_Server/src/controller", "DartGame_Server/src/controllers")
if os.path.exists("DartGame_Server/src/service"):
    os.rename("DartGame_Server/src/service", "DartGame_Server/src/services")

# 3. Create missing classes in Client
client_missing = {
    "DartGame_Client/src/models/GameViewState.java": "package models;\n\npublic class GameViewState {}\n",
    "DartGame_Client/src/models/RankingData.java": "package models;\n\npublic class RankingData {}\n",
}
for filepath, content in client_missing.items():
    if not os.path.exists(filepath):
        with open(filepath, 'w') as f:
            f.write(content)

# 4. Create missing classes in Server
server_missing = {
    "DartGame_Server/src/models/GameState.java": "package models;\n\npublic class GameState {}\n",
    "DartGame_Server/src/models/ThrowResult.java": "package models;\n\npublic class ThrowResult {}\n",
    "DartGame_Server/src/models/Invitation.java": "package models;\n\npublic class Invitation {}\n",
    "DartGame_Server/src/models/Bot.java": "package models;\n\npublic class Bot {}\n",
    "DartGame_Server/src/controllers/GameController.java": "package controllers;\n\npublic class GameController {}\n",
    "DartGame_Server/src/controllers/MatchHistoryController.java": "package controllers;\n\npublic class MatchHistoryController {}\n",
    "DartGame_Server/src/services/DartboardScorer.java": "package services;\n\npublic class DartboardScorer {}\n",
    "DartGame_Server/src/services/GameService.java": "package services;\n\npublic class GameService {}\n",
    "DartGame_Server/src/services/BotService.java": "package services;\n\npublic class BotService {}\n",
}
for filepath, content in server_missing.items():
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    if not os.path.exists(filepath):
        with open(filepath, 'w') as f:
            f.write(content)

# 5. Delete obsolete files
obsolete_files = [
    "DartGame_Client/src/controllers/StartGameWithBotController.java",
    "DartGame_Client/src/controllers/ChooseModeController.java",
    "DartGame_Client/src/views/choosemode.fxml",
    "DartGame_Client/src/views/startgamewithbot.fxml"
]
for f in obsolete_files:
    if os.path.exists(f):
        os.remove(f)

# Rename LeaderboardController to RankingController if exists
if os.path.exists("DartGame_Server/src/controllers/LeaderboardController.java"):
    os.rename("DartGame_Server/src/controllers/LeaderboardController.java", "DartGame_Server/src/controllers/RankingController.java")

print("Restructuring completed.")
