# Hợp đồng class — Darts Battle

Tài liệu này là hợp đồng triển khai giữa bốn thành viên. Mỗi class có một người sở hữu. Thành viên khác chỉ gọi hàm công khai đã nêu; không tự sửa class, message hoặc dữ liệu công khai của phần người khác.

Common và schema database được cả nhóm cùng chốt trong tuần 1. Sau đó Người 1 quản lý code Common, Người 4 quản lý file SQL của trận. Mọi thay đổi phải cập nhật `PROTOCOL.md` trước khi sửa code.

## 1. DartGame_Common

### `protocol/Message.java` — Người 1

- Một gói JSON truyền trên Socket.
- Field: `MessageType type`, `String requestId`, `Object payload`.
- Hàm: constructor, getter/setter, `static Message of(MessageType type, Object payload)`.
- Không chứa Socket, JavaFX, luật game hoặc SQL.

### `protocol/MessageType.java` — Người 1, cả nhóm duyệt

`REGISTER_REQUEST`, `REGISTER_RESULT`, `LOGIN_REQUEST`, `LOGIN_RESULT`, `LOGOUT_REQUEST`, `LOGOUT_RESULT`, `ONLINE_LIST`, `INVITE_REQUEST`, `INVITE_RECEIVED`, `INVITE_ACCEPT`, `INVITE_REJECT`, `INVITE_RESULT`, `CREATE_BOT_MATCH`, `MATCH_STARTED`, `TURN_PREPARING`, `ROTATION_SUBMIT`, `ROTATION_APPLIED`, `TURN_STARTED`, `THROW_COORDINATE`, `THROW_RESOLVED`, `MATCH_FINISHED`, `CHAT_MESSAGE`, `LEAVE_MATCH`, `GET_RANKING`, `RANKING_RESULT`, `GET_HISTORY`, `HISTORY_RESULT`, `ERROR`.

Chỉ Người 1 sửa enum sau khi cả nhóm đã thống nhất `PROTOCOL.md`.

### DTO — Người 1 tạo khung, người phụ trách chức năng duyệt field

`LoginRequest`

- Field: `String username`, `String password`.

`ThrowCoordinateRequest`

- Field: `String matchId`, `long turnId`, `double x`, `double y`.
- Không có `score`, `remainingScore` hoặc `hitArea`.

`ThrowResolvedResponse`

- Field: `String matchId`, `long turnId`, `String actorName`, `ActorType actorType`, `int dartIndex`, `double x`, `double y`, `int rotationDegree`, `HitArea hitArea`, `int dartScore`, `int remainingScoreAfter`, `boolean isBust`, `boolean isTurnFinished`, `boolean isMatchFinished`.
- Chỉ server tạo; client chỉ hiển thị.

Các payload khác phải có field cụ thể trong `PROTOCOL.md`. Khi cả nhóm thống nhất cần thêm DTO mới thì mới thêm file vào `protocol/dto/` và cập nhật đồng thời cây thư mục trong `chiaviec.md`.

### Enums

`PlayerStatus.java` — `OFFLINE`, `IDLE`, `INVITED`, `IN_MATCH`.

`MatchMode.java` — `PVP`, `PVE`.

`MatchStatus.java` — `WAITING_ROTATION`, `PLAYING`, `FINISHED`.

`ActorType.java` — `HUMAN`, `BOT`.

`HitArea.java` — `MISS`, `SINGLE`, `DOUBLE`, `TRIPLE`, `OUTER_BULL`, `BULLSEYE`.

### `config/DartboardLayout.java` — Người 3 chốt quy tắc, Người 1 cập nhật Common

- Hằng số hệ tọa độ chung: tâm bàn, bán kính ngoài, bán kính bullseye, outer bull, các biên vòng double và triple.
- Mảng thứ tự 20 ô: `{20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5}`.
- Chỉ chứa hằng số để client vẽ và server dùng cùng hệ tọa độ; không có hàm tính điểm.

## 2. DartGame_Client

### Models

`models/User.java` — Người 1

- Field: `long id`, `String username`, `PlayerStatus status`.
- Dùng cho user đang đăng nhập và dòng danh sách online; không chứa password, password hash hay điểm 301.

`models/GameViewState.java` — Người 3

- Bản sao trạng thái để hiển thị, không phải trạng thái chính thức.
- Field: `String matchId`, `MatchMode mode`, `String playerName`, `String opponentName`, `int playerRemaining`, `int opponentRemaining`, `String currentActorName`, `ActorType currentActorType`, `long turnId`, `int rotationDegree`, `int dartsThrown`.
- Hàm: `reset()`, `startMatch(...)`, `startTurn(...)`, `applyThrow(ThrowResolvedResponse result)`, getter/setter.

`models/RankingData.java` — Người 4

- Field: `int rank`, `String username`, `int pvpWins`.

`models/MatchHistoryData.java` — Người 4

- Field: `String matchId`, `MatchMode mode`, `String opponentName`, `String result`, `String endReason`, `LocalDateTime endedAt`.

### Controllers

`controllers/SocketHandler.java` — Người 1

- Field: `Socket socket`, luồng vào/ra, `User currentUser`.
- Hàm: `connect(host, port)`, `send(Message)`, `listen()`, `disconnect()`, `handleMessage(Message)`.
- Chuyển message đến controller phù hợp trên JavaFX Application Thread bằng `Platform.runLater`.
- Không tính điểm, không chứa SQL, không chứa code FXML.

`controllers/LoginController.java` — Người 1

- Điều khiển `login.fxml`; kiểm tra form, gửi `LOGIN_REQUEST`, nhận `LOGIN_RESULT`.

`controllers/RegisterController.java` — Người 1

- Điều khiển `register.fxml`; kiểm tra form, gửi `REGISTER_REQUEST`, nhận `REGISTER_RESULT`.

`controllers/HomeController.java` — Người 1

- Điều khiển `home.fxml`; điều hướng Lobby, Ranking, History và gửi `LOGOUT_REQUEST`.

`controllers/ChooseOpponentController.java` — Người 2

- Điều khiển `chooseopponent.fxml`.
- Nhận `ONLINE_LIST`, cập nhật `TableView<User>`; gửi `INVITE_REQUEST` khi chọn người `IDLE`.
- Nút chơi với máy gửi `CREATE_BOT_MATCH`.

`controllers/NotificationController.java` — Người 2

- Hiển thị `INVITE_RECEIVED`.
- Hàm `accept()` gửi `INVITE_ACCEPT`; `reject()` gửi `INVITE_REJECT`.

`controllers/StartGameController.java` — Người 3

- Controller duy nhất cho `startgame.fxml`, dùng chung PvP và PvE.
- Vẽ Dartboard theo `DartboardLayout`; điều khiển hai vạch X/Y để lấy `(x, y)`.
- Hiển thị Bot nếu `GameViewState.mode = PVE`; Người 4 không sửa class này.
- Hàm: `prepareTurn()`, `submitRotation()`, `startXAxis()`, `stopXAxis()`, `startYAxis()`, `stopYAxis()`, `submitCoordinate()`, `applyRotation()`, `showThrowResult()`, `showMatchFinished()`.
- Chỉ gửi `ROTATION_SUBMIT` và `THROW_COORDINATE`; không tự tính điểm.

`controllers/RankingController.java` — Người 4

- Gửi `GET_RANKING`, nhận `RANKING_RESULT`, cập nhật `TableView<RankingData>`.

`controllers/MatchHistoryController.java` — Người 4

- Gửi `GET_HISTORY`, nhận `HISTORY_RESULT`, cập nhật `TableView<MatchHistoryData>`.

### Views và resources

- Mỗi FXML gắn với đúng một controller.
- Chỉ có `startgame.fxml`, không tạo `StartGameWithBotController`.
- `images/` và `musics/` chỉ chứa tài nguyên; Người 4 quản lý nhạc/âm thanh.

## 3. DartGame_Server

### Models

`models/User.java` — Người 1

- Field: `long id`, `String username`, `String passwordHash`.
- Chỉ là model phía server; không truyền password hash về client.

`models/Room.java` — Người 2

- Đại diện một trận đang hoạt động trong RAM.
- Field: `String matchId`, `long player1Id`, `Long player2Id`, `ActorType player2Type`, `MatchMode mode`, `GameState gameState`.
- Hàm: `containsUser(long userId)`, `getOpponentUserId(long userId)`, `getGameState()`.
- Không chứa SQL; bị xóa khỏi RAM khi trận kết thúc.

`models/GameState.java` — Người 3

- Trạng thái chính thức của trận; chỉ `GameService` được sửa.
- Field: `int player1RemainingScore`, `int player2RemainingScore`, `Long currentPlayerId`, `ActorType currentActorType`, `long turnId`, `int dartsThrown`, `int rotationDegree`, `MatchStatus status`.
- `currentPlayerId = null` khi `currentActorType = BOT`.
- Hàm: `startTurn(...)`, `setRotation(...)`, `recordThrow(...)`, `switchTurn()`, `finish()`.
- Mọi thay đổi phải nằm trong khóa đồng bộ của Room hoặc các hàm `synchronized`.
- Không dùng `turnStartScore`, vì BUST của nhóm áp dụng riêng cho từng phi tiêu.

`models/ThrowResult.java` — Người 3

- Kết quả một phi tiêu sau khi GameService xác thực.
- Field tương ứng với `ThrowResolvedResponse`, gồm actor, tọa độ, điểm mũi, điểm còn lại, BUST, đổi lượt và kết thúc trận.

`models/Invitation.java` — Người 2

- Field: `String invitationId`, `long senderUserId`, `long receiverUserId`, `LocalDateTime expiresAt`, trạng thái `PENDING`, `ACCEPTED`, `REJECTED`, `EXPIRED`.

`models/Bot.java` — Người 4

- Field: `String name`, mức độ khó, các tham số độ chính xác.
- Hàm: sinh góc xoay và tọa độ mục tiêu.
- Không có điểm còn lại, không tự trừ điểm.

### Controllers

`controllers/UserController.java` — Người 1

- Nhận `REGISTER_REQUEST`, `LOGIN_REQUEST`, `LOGOUT_REQUEST`.
- Dùng `DBConnection`; hash/verify password; trả `REGISTER_RESULT`, `LOGIN_RESULT`, `LOGOUT_RESULT`.
- Chỉ chứa SQL liên quan bảng `users`.

`controllers/LobbyController.java` — Người 2

- Xử lý danh sách người chơi online, `INVITE_REQUEST`, `INVITE_ACCEPT`,
  `INVITE_REJECT`, `CHAT_MESSAGE` và `CREATE_BOT_MATCH`.
- Gọi `ClientManager` để kiểm tra trạng thái người chơi và gửi message.
- Gọi `RoomManager` để tạo Room PvP hoặc Room PvE.
- Khi lời mời PvP được chấp nhận, gọi `RoomManager.createPvpRoom()`,
  sau đó gọi `GameService.startMatch(room)` để khởi tạo GameState và bắt đầu trận.
- Khi nhận `CREATE_BOT_MATCH`, gọi `RoomManager.createPveRoom()`,
  sau đó gọi `GameService.startMatch(room)` để khởi tạo GameState và bắt đầu trận.
- Chuyển tiếp `CHAT_MESSAGE` đến đối thủ trong cùng Room.
- Không tính điểm, không xử lý tọa độ ném và không quyết định thắng/thua.

`controllers/GameController.java` — Người 3

- Nhận `ROTATION_SUBMIT`, `THROW_COORDINATE`, `LEAVE_MATCH`.
- Kiểm tra client thuộc Room rồi gọi `GameService`; không trực tiếp tính tọa độ hoặc ghi SQL.

`controllers/RankingController.java` — Người 4

- Nhận `GET_RANKING`, dùng `DBConnection` truy vấn số trận thắng PvP từ `matches`, trả `RANKING_RESULT`.

`controllers/MatchHistoryController.java` — Người 4

- Nhận `GET_HISTORY`, dùng `DBConnection` truy vấn lịch sử từ `matches`, trả `HISTORY_RESULT`.
- Cung cấp các hàm công khai để `GameService` gọi: `createMatch(...)`, `saveThrow(...)`, `finishMatch(...)`.
- Đây là nơi duy nhất chứa SQL liên quan `matches` và `match_throws`.

### Services

`services/Client.java` — Người 1

- Đại diện một kết nối client trên server.
- Field: Socket/stream, `long loggedInUserId`, `String loggedInUsername`.
- Hàm: `run()`, `send(Message)`, `close()`, getter user.
- Đọc JSON, chuyển message đến controller; không chứa luật game.

`services/ClientManager.java` — Người 2

- Lưu Client đang kết nối theo `userId`.
- Hàm: `add()`, `remove()`, `findByUserId()`, `broadcastOnlineList()`, `sendToUser()`.
- Là nguồn trạng thái online; `PlayerStatus` được quản lý tại đây.

`services/RoomManager.java` — Người 2

- Lưu Room đang hoạt động theo `matchId` và ánh xạ `userId → matchId`.
- Hàm: `createPvpRoom()`, `createPveRoom()`, `find()`, `remove()`, `findRoomOfUser()`.

`services/DartboardScorer.java` — Người 3

- Hàm thuần: `HitArea calculateHitArea(double x, double y, int rotationDegree)` và `int calculateDartScore(double x, double y, int rotationDegree)`.
- Tính ngoài bàn, bull, lát số, single/double/triple.
- Đây là class duy nhất chuyển tọa độ thành `HitArea` và điểm thô; `GameService` dùng kết quả đó để tạo `ThrowResult`.
- Không biết lượt, điểm còn lại, BUST hay database.

`services/GameService.java` — Người 3

- Là nơi duy nhất sửa `GameState`.
- Hàm: `startMatch(Room room)`, `submitRotation(...)`, `applyBotRotation(...)`, `submitThrow(...)`, `leaveMatch(...)`, `handleDisconnect(...)`, `finishMatch(...)`.
- Kiểm tra đúng Room, đúng lượt, turnId, góc xoay, tối đa ba phi tiêu.
- Gọi `DartboardScorer`; áp dụng luật 301, BUST từng phi tiêu, đổi lượt và thắng đúng 0.
- Gửi `TURN_PREPARING`, `ROTATION_APPLIED`, `TURN_STARTED`, `THROW_RESOLVED`, `MATCH_FINISHED` qua `ClientManager`.
- Gọi các hàm công khai `createMatch(...)`, `saveThrow(...)`, `finishMatch(...)` của `MatchHistoryController`; không tự viết SQL.

`services/BotService.java` — Người 4

- Khi lượt chuyển sang Bot, sinh một góc xoay từ 1 đến 359 và tối đa ba tọa độ ném.
- Gọi `GameService.applyBotRotation(...)`, sau đó gọi `GameService.submitThrow(...)` cho từng phi tiêu.
- Có thể tạo trễ 1–2 giây để người chơi nhìn thấy Bot ném.
- Không tự sửa GameState, không tự tính điểm, không tự gửi kết quả client.

### Database

`database/DBConnection.java` — Người 1

- Tạo và cung cấp kết nối MySQL; không chứa SQL nghiệp vụ.

`dartgame_server/DartGame_Server.java` — Người 1

- Điểm khởi động: mở ServerSocket, khởi tạo manager, service, controller và nhận kết nối mới.

## 4. Ranh giới trách nhiệm bắt buộc

- Client không tính điểm, không sửa điểm 301, không quyết định thắng/thua.
- `SocketHandler` và `Client` chỉ truyền/điều phối message.
- Chỉ `GameService` sửa `GameState`.
- Chỉ `DartboardScorer` đổi tọa độ thành vùng điểm và điểm thô.
- `BotService` chỉ sinh dữ liệu đầu vào cho Bot rồi gọi `GameService`.
- Chỉ server được truy cập MySQL.
- `Room` chỉ tồn tại trong RAM; không có bảng `rooms`.
- Khi trận bắt đầu, `MatchHistoryController.createMatch()` tạo một bản ghi `matches`; mỗi phi tiêu server xác thực được `MatchHistoryController.saveThrow()` lưu vào `match_throws`; khi trận kết thúc, `MatchHistoryController.finishMatch()` cập nhật người thắng, lý do và thời điểm kết thúc.
- Database chỉ có `users`, `matches`, `match_throws`; không dùng `match_players`.
