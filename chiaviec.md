1. Mục tiêu bản mới
Làm game Darts Battle bằng JavaFX, Socket TCP và MySQL, gồm:
PvP: hai người chơi thật đấu với nhau qua server.
PvE: người chơi đấu với Bot do server điều khiển.
Đăng ký, đăng nhập, danh sách online.
Mời chơi, Accept/Reject, tạo phòng.
Dartboard tương tác, xoay bàn bắt buộc, ném bằng hai vạch X/Y.
Chat, rời trận, mất kết nối.
Lịch sử đấu và bảng xếp hạng.
Nhạc nền, âm thanh ném/thắng/thua.
Khác biệt quan trọng với source cũ:
Client chỉ gửi tọa độ ném. Server kiểm tra lượt, tính vùng điểm, cập nhật điểm còn lại và gửi kết quả đã xác thực về hai client.

2. Luật chơi chốt
Mỗi người chơi bắt đầu với 301 điểm riêng.
Hai bên luân phiên lượt.
Mỗi lượt ném tối đa 3 phi tiêu.
Mỗi lượt phải nhập một góc xoay 1–359°; bàn quay góc đó rồi mới ném.
Cách ném:Vạch X chạy, bấm để chốt X.
Vạch Y chạy, bấm để chốt Y.
Giao điểm là tọa độ phi tiêu.

Vùng điểm:Single: điểm gốc.
Double: điểm gốc × 2.
Triple: điểm gốc × 3.
Outer bull: 25 điểm.
Bullseye: 50 điểm.
Ngoài bàn: 0 điểm.

Điểm giảm từ 301 về đúng 0 là thắng.
Nếu điểm một phi tiêu lớn hơn điểm còn lại: không trừ điểm, vẫn tiếp tục các phi tiêu còn lại trong lượt.
Khi về 0 ở phi tiêu thứ 1 hoặc 2 thì thắng ngay, không cần ném tiếp.
Người rời trận/mất kết nối bị xử thua; đối thủ thắng.
Điểm trong trận và thành tích xếp hạng là hai khái niệm khác nhau:
301 chỉ dùng trong một trận, giảm về đúng 0 để thắng.
Bảng xếp hạng PvP được sắp xếp theo tổng số trận thắng PvP giảm dần.
Tổng số trận thắng được tính từ bảng matches, không lưu trùng trong bảng users.
PvE có lưu lịch sử nhưng không được tính vào bảng xếp hạng PvP.
3. Cấu trúc project
Dùng ba project nhỏ: Client và Server là chính, Common chỉ chứa dữ liệu dùng chung.
```
Darts-Battle/
├── DartGame_Common/
├── DartGame_Client/
└── DartGame_Server/
DartGame_Common/
└── src/
    ├── protocol/
    │   ├── Message.java
    │   ├── MessageType.java
    │   └── dto/
    │       ├── LoginRequest.java
    │       ├── ThrowCoordinateRequest.java
    │       └── ThrowResolvedResponse.java
    ├── enums/
    │   ├── PlayerStatus.java
    │   ├── MatchMode.java
    │   ├── MatchStatus.java
    |   |── ActorType.java
    │   └── HitArea.java
    └── config/
        └── DartboardLayout.java
DartGame_Client/
└── src/
    ├── models/
    │   ├── User.java
    │   ├── GameViewState.java
    │   ├── RankingData.java
    │   └── MatchHistoryData.java
    ├── controllers/
    │   ├── SocketHandler.java
    │   ├── LoginController.java
    │   ├── RegisterController.java
    │   ├── HomeController.java
    │   ├── ChooseOpponentController.java
    │   ├── NotificationController.java
    │   ├── StartGameController.java
    │   ├── RankingController.java
    │   └── MatchHistoryController.java
    ├── views/
    │   └── các file .fxml
    ├── images/
    └── musics/
DartGame_Server/
└── src/
    ├── models/
    │   ├── User.java
    │   ├── Room.java
    │   ├── GameState.java
    │   ├── ThrowResult.java
    │   ├── Invitation.java
    │   └── Bot.java
    ├── controllers/
    │   ├── UserController.java
    │   ├── LobbyController.java
    │   ├── GameController.java
    │   ├── RankingController.java
    │   └── MatchHistoryController.java
    ├── services/
    │   ├── Client.java
    │   ├── ClientManager.java
    │   ├── RoomManager.java
    │   ├── GameService.java
    |   |── DartboardScorer.java
    │   └── BotService.java
    ├── database/
    │   └── DBConnection.java
    └── dartgame_server/
        └── DartGame_Server.java
```
Common không chứa GameService hoặc điểm trận chính thức. Server là nơi duy nhất được quyền quyết định luật và dữ liệu trận.
4. MVC và luồng xử lý
Client theo MVC cơ bản:
“View FXML → Client Controller → SocketHandler → Server
Server Client/Controller/Service xử lý → gửi Message về client
SocketHandler phía Client nhận Message → chuyển về JavaFX Application Thread → Client Controller cập nhật Model và View FXML.”
Ví dụ khi ném:
StartGameController
  → gửi THROW_COORDINATE(matchId, turnId, x, y)
  → GameController phía Server
  → GameService tính điểm và cập nhật GameState
  → Server gửi THROW_RESOLVED đến hai client
  → SocketHandler nhận kết quả
  → StartGameController cập nhật điểm/giao diện
5. Các message chính
Message	Hướng gửi	Mục đích
REGISTER_REQUEST    Client → Server    Gửi thông tin đăng ký
REGISTER_RESULT     Server → Client    Kết quả đăng ký

LOGIN_REQUEST       Client → Server    Gửi thông tin đăng nhập
LOGIN_RESULT        Server → Client    Kết quả đăng nhập và thông tin người dùng
LOGOUT_REQUEST      Client → Server    Yêu cầu đăng xuất
LOGOUT_RESULT       Server → Client    Xác nhận đăng xuất
ONLINE_LIST	Server → Client	Danh sách người online
INVITE_REQUEST      Client → Server   Người chơi gửi lời mời PvP
INVITE_RECEIVED     Server → Client   Thông báo lời mời đến người được mời
INVITE_ACCEPT       Client → Server   Người được mời chấp nhận
INVITE_REJECT       Client → Server   Người được mời từ chối
INVITE_RESULT       Server → Client   Báo kết quả lời mời cho hai bên
CREATE_BOT_MATCH	Client → Server	Tạo trận PvE
MATCH_STARTED        Server → Client   Vào trận
TURN_PREPARING       Server → Client   Yêu cầu người đến lượt nhập góc xoay
ROTATION_SUBMIT      Client → Server   Gửi góc xoay từ 1 đến 359
ROTATION_APPLIED     Server → Client   Đồng bộ góc xoay đã được xác thực cho hai bên
TURN_STARTED         Server → Client   Báo người được ném và số phi tiêu còn lại trong lượt
THROW_COORDINATE     Client → Server   Gửi tọa độ ném
THROW_RESOLVED       Server → Client   Kết quả mũi ném đã được xác thực
MATCH_FINISHED	Server → Client	Kết thúc trận
CHAT_MESSAGE	Hai chiều	Chat PvP
LEAVE_MATCH	Client → Server	Rời trận
GET_RANKING       Client → Server   Yêu cầu bảng xếp hạng PvP
RANKING_RESULT    Server → Client   Trả bảng xếp hạng PvP

GET_HISTORY       Client → Server   Yêu cầu lịch sử trận của người chơi
HISTORY_RESULT    Server → Client   Trả lịch sử PvP và PvE
ERROR    Server → Client    Báo lỗi: sai lượt, góc xoay không hợp lệ, tọa độ không hợp lệ, trận không tồn tại


Cả nhóm vẫn phải viết PROTOCOL.md để mô tả mỗi message, field, bên gửi và phản hồi dự kiến.
6. Database
users
- id
- username
- password_hash
- created_at

matches
id
mode: PVP hoặc PVE
player1_id: khóa ngoại đến users.id
player2_id: khóa ngoại đến users.id, có thể NULL nếu là PvE
player2_type: HUMAN hoặc BOT
winner_id: khóa ngoại đến users.id, có thể NULL nếu Bot thắng
winner_type: HUMAN hoặc BOT
end_reason: ZERO_SCORE / FORFEIT / DISCONNECT
started_at
ended_at

match_throws
id
match_id: khóa ngoại đến matches.id
player_id: khóa ngoại đến users.id, có thể NULL nếu là Bot
actor_type: HUMAN hoặc BOT
turn_number
dart_number: 1, 2 hoặc 3
x
y
rotation_degree
hit_area: MISS / SINGLE / DOUBLE / TRIPLE / OUTER_BULL / BULLSEYE
dart_score
remaining_score_after
created_at

Với PvE:
player1_id là người thật.
player2_id = NULL.
player2_type = BOT.
Khi Bot thắng: winner_id = NULL, winner_type = BOT.
Các lần Bot ném có player_id = NULL, actor_type = BOT
## 7. Chia việc cho 4 người

Nguyên tắc: mỗi class chỉ có một người phụ trách chính. Thành viên khác
chỉ sử dụng các hàm công khai đã thống nhất, không tự ý sửa class của người khác.

| Người | Client | Server | Database / Test |
|---|---|---|---|
| Người 1 | `LoginController`, `RegisterController`, `HomeController`, `SocketHandler`, `User` | `DartGame_Server`, `Client`, `UserController`, `User` | Thiết kế chung database; phụ trách bảng `users`; test đăng ký, đăng nhập, hash mật khẩu, kết nối và đăng nhập trùng |
| Người 2 | `ChooseOpponentController`, `NotificationController`, phần chat trong giao diện trận | `LobbyController`, `ClientManager`, `RoomManager`, `Room`, `Invitation` | Test danh sách online, trạng thái, lời mời, Accept/Reject, timeout, Room, chat và disconnect |
| Người 3 | `StartGameController`, `GameViewState`, Dartboard, hai vạch X/Y, xoay bàn, bảng điểm và hiển thị PvP/PvE | `GameController`, `GameService`, `DartboardScorer`, `GameState`, `ThrowResult` | Test vùng điểm, góc xoay, đúng lượt, tối đa ba phi tiêu, BUST, đổi lượt và thắng khi về 0 |
| Người 4 | `RankingController`, `MatchHistoryController`, `RankingData`, `MatchHistoryData`, nhạc nền/âm thanh | `Bot`, `BotService`, `RankingController`, `MatchHistoryController`;
`MatchHistoryController` lưu/truy vấn `matches` và `match_throws` | Thiết kế chung database; phụ trách `matches`, `match_throws`; test Bot, PvE, lịch sử, ranking và lưu kết quả bỏ cuộc/mất kết nối |

### Người 1 – Tài khoản và kết nối

**Client**

- `SocketHandler`: kết nối TCP, gửi/nhận JSON, chuyển message đến controller.
- `LoginController`: gửi yêu cầu đăng nhập, nhận kết quả.
- `RegisterController`: gửi yêu cầu đăng ký.
- `HomeController`: điều hướng từ màn hình chính đến lobby, ranking và history.
Chức năng chơi với Bot nằm trong `ChooseOpponentController`.
- `User`: lưu dữ liệu người dùng đang đăng nhập.

**Server**

- `DartGame_Server`: mở `ServerSocket`, nhận kết nối mới.
- `Client`: đại diện cho một socket client; đọc/ghi message.
- `UserController`: đăng ký, đăng nhập, đăng xuất; kiểm tra và mã hóa mật khẩu.

**Database/Test**

- Phụ trách bảng `users`.
- Test: username trùng, sai mật khẩu, đăng nhập thành công, nhiều client kết nối.

---

### Người 2 – Lobby, lời mời, Room và chat

**Client**

- `ChooseOpponentController`: danh sách online, chọn đối thủ, gửi lời mời;
nút Chơi với Bot gửi `CREATE_BOT_MATCH`.
- `NotificationController`: popup nhận lời mời; Accept/Reject.
- Phần chat trong giao diện trận: gửi/hiển thị `CHAT_MESSAGE`.

**Server**

- `ClientManager`: quản lý client online và trạng thái OFFLINE, IDLE,INVITED, IN_MATCH`.
- `LobbyController`: xử lý danh sách online, gửi/chấp nhận/từ chối lời mời, chat.
- `RoomManager`: tạo, tìm và xóa `Room` trong RAM.
- `Room`: chứa hai bên đang chơi và `GameState` của trận đang diễn ra.
- `Invitation`: thông tin lời mời và trạng thái chờ/chấp nhận/từ chối.
- `LobbyController`: nhận `CREATE_BOT_MATCH`, yêu cầu `RoomManager`
  tạo Room PvE cho người chơi và Bot.

**Test**

- Không mời chính mình hoặc người đang trong trận.
- Không tạo trùng phòng.
- Accept tạo Room; Reject không tạo Room.
- Client mất kết nối: báo đối thủ, giải phóng Room.
- `Room` chỉ ở RAM, không có bảng `rooms` trong database.

---

### Người 3 – Luật game và Dartboard

**Client**

- `StartGameController`: màn hình chơi dùng chung cho PvP và PvE.
- Dartboard tương tác: lấy tọa độ X/Y và gửi `THROW_COORDINATE`.
- Chức năng xoay bàn: gửi `ROTATION_SUBMIT`.
- Hiển thị điểm còn lại, lượt, số phi tiêu, BUST và kết quả trận.
- Client không tự tính điểm; chỉ hiển thị kết quả server trả về.

**Server**

- `GameController`: nhận `ROTATION_SUBMIT`, `THROW_COORDINATE`, `LEAVE_MATCH`.
- `GameService`: kiểm tra đúng lượt, giới hạn ba phi tiêu, cập nhật điểm và đổi lượt.
- `DartboardScorer`: tính vùng trúng và điểm từ tọa độ cùng góc xoay.
- `GameState`: trạng thái chính thức của trận trong `Room`.
- `ThrowResult`: kết quả một phi tiêu đã được server xác thực.

**Test**

- `MISS`, `SINGLE`, `DOUBLE`, `TRIPLE`, `OUTER_BULL`, `BULLSEYE`.
- Mỗi lượt tối đa ba phi tiêu.
- Sai lượt bị từ chối.
- Điểm ném lớn hơn điểm còn lại là BUST riêng mũi đó.
- Về đúng 0 thì thắng ngay.
- Góc xoay ảnh hưởng đúng đến ô điểm.

---

### Người 4 – Bot, lịch sử và xếp hạng

**Client**
- `RankingController`: hiển thị bảng xếp hạng PvP.
- `MatchHistoryController`: hiển thị lịch sử PvP và PvE.
- Bật/tắt nhạc nền tại client.

**Server**

- `Bot`: cấu hình độ khó và tham số sinh tọa độ ném.
- `BotService`: khi đến lượt Bot, tự sinh góc xoay hợp lệ từ 1 đến 359
  và tối đa ba tọa độ ném. BotService gọi các hàm công khai của
  `GameService` để áp dụng góc xoay và xử lý từng phi tiêu.
  `GameService` là nơi duy nhất được phép cập nhật `GameState`, tính điểm,
  đổi lượt và gửi kết quả về client.
- `RankingController`: truy vấn số trận thắng PvP từ bảng `matches`.
- `MatchHistoryController`: lưu và truy vấn lịch sử trận.
- Phối hợp với Người 3 khi xử lý `LEAVE_MATCH`; lưu `end_reason`
  là `FORFEIT` hoặc `DISCONNECT`.

**Database/Test**

- Phụ trách bảng `matches` và `match_throws`.
- `matches` lưu hai bên chơi, chế độ, người thắng, lý do và thời gian trận.
- `match_throws` lưu từng phi tiêu đã được server xác thực.
- Với Bot: `player2_id`, `winner_id` hoặc `player_id` có thể là `NULL`;
  cột `player2_type`, `winner_type`, `actor_type` sẽ ghi là `BOT`.
- Test: Bot ném đúng luồng luật game, lưu lịch sử đầy đủ, ranking chỉ tính PvP,
  rời trận và mất kết nối được lưu đúng lý do.



8. Công việc chi tiết từng người

Người 1

Tạo ba project DartGame_Common, DartGame_Client và DartGame_Server.
Thiết lập kết nối Socket TCP giữa client và server.
Viết DartGame_Server để mở ServerSocket và nhận nhiều client.
Viết Client phía server để đọc, ghi Message JSON với từng client.
Viết SocketHandler phía client để gửi/nhận Message trên luồng nền.
Đăng ký, đăng nhập và đăng xuất.
Kiểm tra username trùng.
Hash mật khẩu trước khi lưu database.
Chống một tài khoản đăng nhập đồng thời trên hai client.
Quản lý thông tin User đang đăng nhập ở client.
Phụ trách bảng users.
Test đăng ký, sai mật khẩu, username trùng, đăng nhập đồng thời và nhiều client kết nối.

Người 2

Hiển thị danh sách người chơi online và trạng thái OFFLINE, IDLE, INVITED, IN_MATCH.
Gửi INVITE_REQUEST và hiển thị INVITE_RECEIVED.
Làm popup Accept và Reject.
Xử lý INVITE_ACCEPT, INVITE_REJECT, INVITE_RESULT và timeout lời mời.
Không cho mời chính mình hoặc người chơi đang trong trận.
Tạo Room khi lời mời được chấp nhận.
Nhận CREATE_BOT_MATCH và tạo Room PvE cho người chơi với Bot.
Quản lý Room trong RAM bằng RoomManager.
Xóa Room khi trận kết thúc hoặc client mất kết nối.
Làm giao diện và chức năng chat PvP bằng CHAT_MESSAGE.
Phát hiện client disconnect, thông báo cho đối thủ và chuyển thông tin Room cho GameService xử lý kết quả.
Test danh sách online, trạng thái người chơi, lời mời, accept/reject, timeout, phòng trùng, chat và disconnect.

Người 3

Làm StartGameController dùng chung cho PvP và PvE.
Vẽ Dartboard và cơ chế hai vạch X/Y để lấy tọa độ ném.
Làm giao diện nhập góc xoay và gửi ROTATION_SUBMIT.
Chỉ cho phép người đang đến lượt thao tác giao diện ném.
Hiển thị phi tiêu của cả người chơi và đối thủ từ kết quả server gửi về.
Viết GameController nhận ROTATION_SUBMIT, THROW_COORDINATE và LEAVE_MATCH.
Viết GameService kiểm tra đúng trận, đúng lượt, đúng turnId và tối đa ba phi tiêu mỗi lượt.
Viết DartboardScorer xác định MISS, SINGLE, DOUBLE, TRIPLE, OUTER_BULL, BULLSEYE từ x, y và rotationDegree.
Quản lý GameState: điểm khởi tạo 301, lượt hiện tại, số phi tiêu, góc xoay, BUST, đổi lượt và kết thúc trận.
Khởi tạo GameState cho cả Room PvP và Room PvE.
Khi người chơi rời trận hoặc mất kết nối, GameService xác định đối thủ là người thắng.
Server gửi THROW_RESOLVED cho cả hai client sau mỗi phi tiêu.
Test toàn bộ luật điểm, BUST, sai lượt, ném quá ba lần, xoay bàn và thắng khi về đúng 0.

Người 4

Viết Bot và BotService cho chế độ PvE.
Phối hợp với Người 2 để thống nhất luồng CREATE_BOT_MATCH và với Người 3 để thống nhất dữ liệu Bot gửi vào GameService.
Khi đến lượt Bot, BotService tự sinh góc xoay hợp lệ từ 1 đến 359 và tối đa ba tọa độ ném.
BotService gọi GameService để áp dụng góc xoay và xử lý từng phi tiêu.
GameService là bên gửi ROTATION_APPLIED và THROW_RESOLVED đến client.
Bot không được tự cộng/trừ điểm hoặc tự sửa GameState.
Viết RankingController phía server và client.
Bảng xếp hạng chỉ tính trận PVP, sắp xếp theo tổng số trận thắng giảm dần.
Viết MatchHistoryController phía server và client.
Lưu trận vào matches, lưu từng phi tiêu đã xác thực vào match_throws.
Lưu đúng end_reason là ZERO_SCORE, FORFEIT hoặc DISCONNECT.
Làm nhạc nền, âm thanh ném, thắng và thua ở client.
Test Bot, lịch sử PvP/PvE, bảng xếp hạng, bỏ cuộc và mất kết nối.

Luồng phối hợp khi người chơi rời trận hoặc mất kết nối:
Người 2 phát hiện sự kiện và xác định Room.
Người 3 dùng GameService để kết thúc trận và xác định bên thắng.
Người 4 cập nhật matches và lưu lịch sử kết quả.
9. Quy tắc làm việc Git
main: bản chạy ổn định.
Mỗi người làm branch riêng.
Không push trực tiếp lên main.
Mọi thay đổi message phải cập nhật PROTOCOL.md.
Mỗi hai ngày phải tích hợp một lần.
Cuối mỗi tuần phải có demo chạy được, không để dồn ghép code vào tuần cuối.