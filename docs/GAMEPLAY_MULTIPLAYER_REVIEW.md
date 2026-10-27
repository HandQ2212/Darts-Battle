> Báo cáo baseline trước khi sửa, giữ lại để đối chiếu. Phần lớn lỗi dưới đây đã được
> xử lý trong v2; xem [trạng thái, hướng dẫn và kiểm thử hiện tại](DESKTOP_MULTIPLAYER_SETUP.md).
> Bộ tái hiện lỗi cũ không còn áp dụng cho API v2; chạy `python3 scripts/desktop.py test`.

# Rà soát core game và kết nối nhiều client

Ngày kiểm tra: 23/10/2026. Phạm vi: mã nguồn Common, Server, Client hiện tại.

## 1. Kết luận và phạm vi kiểm chứng

**Core game chưa ổn để chơi PvP hoàn chỉnh.** Server nhận nhiều socket được, nhưng luồng tìm đối thủ/mời/chấp nhận chưa hoàn thiện; xử lý lượt, đồng bộ điểm và kết thúc trận còn lỗi nghiêm trọng.

- Biên dịch cả server và client với `javac --release 17`, Common và các JAR sẵn có: thành công trên JDK 18 của môi trường kiểm tra.
- Chạy 17 kiểm tra tái hiện lỗi: cả 17 trường hợp đều tái hiện được. Bộ kiểm tra này ghi nhận hành vi baseline trước thay đổi v2.
- Bộ kiểm tra dùng logic thật của game, controller, quản lý phòng/client và scorer; thay phần lưu lịch sử bằng stub không ghi DB, và dùng client thu bản tin trong bộ nhớ. Đây là kiểm tra logic, **chưa phải kiểm thử socket thực tế, giao diện JavaFX, MySQL hay tải nhiều máy**.
- Báo cáo này đề xuất cách xử lý; chưa thay đổi mã nguồn chạy game.

P0: chặn chơi hoặc cho phép sai trạng thái/điểm. P1: gây lệch giao diện, treo trận hoặc ảnh hưởng nhiều phòng. Các mục ghi “tái hiện” đã có kiểm tra; các mục còn lại được xác định qua đọc mã nguồn.

## 2. Các lỗi cần xử lý

### 2.1. P0 — Luồng PvP chưa được triển khai đủ

Vị trí: `DartGame_Server/src/controllers/LobbyController.java:13`, `services/ClientManager.java:55`, `services/Client.java:45`; `DartGame_Client/src/controllers/SocketHandler.java:75` và `:158`.

- `LobbyController` chỉ xử lý `CREATE_BOT_MATCH`; bỏ qua `INVITE_REQUEST`, `INVITE_ACCEPT`, `INVITE_REJECT`, `CHAT_MESSAGE` dù dispatcher đã chuyển đến.
- `broadcastOnlineList()` rỗng. Yêu cầu `ONLINE_LIST` của client cũng không được dispatcher server xử lý. Client nhận danh sách online và lời mời vẫn là phần để trống.
- `createPvpRoom()` tồn tại nhưng không có đường gọi từ lời mời. Nút mời trong giao diện không thể đưa hai người vào một trận PvP qua luồng hiện tại.

**Xử lý:** hoàn thiện chuỗi online → mời → nhận lời mời → chấp nhận/từ chối/hết hạn → tạo đúng một phòng → gửi bắt đầu trận cho hai người. Server lưu invitationId, hai userId, trạng thái và hạn dùng; xác minh người chấp nhận đúng là người nhận. Chat phải kiểm tra thành viên phòng và lấy danh tính từ phiên đăng nhập.

### 2.2. P0 — Người ngoài phòng hoặc sai lượt vẫn ném được

Vị trí: `DartGame_Server/src/services/GameService.java:65`.

`submitThrow()` chỉ kiểm tra phòng tồn tại và trạng thái `PLAYING`. Mọi user không phải player1 đều bị coi là player2. Không kiểm tra đăng nhập, `containsUser()`, `currentPlayerId`, `currentActorType`, `req.turnId`.

**Tái hiện:** user 9999 ném vào phòng khác làm điểm player2 giảm 301 → 251; player2 ném trong lượt player1 vẫn làm tăng số phi tiêu của lượt; turnId 999 vẫn được chấp nhận trong lượt 1.

**Xử lý:** kiểm tra đầy đủ trước khi sửa state: phiên hợp lệ → thành viên → đúng actor → đúng match/turn → đúng giai đoạn → phi tiêu tiếp theo hợp lệ → tọa độ hữu hạn. Lỗi trả `ERROR` có mã cụ thể, không im lặng. Bot dùng đường xử lý nội bộ riêng, không cho client tự xưng là bot.

### 2.3. P0 — Bản tin ném trùng bị tính nhiều lần

Vị trí: `GameService.java:65`; `DartGame_Common/src/protocol/Message.java:8` và `protocol/dto/ThrowCoordinateRequest.java`.

**Tái hiện:** gửi cùng request hai lần làm một người mất 100 điểm và tiêu hao hai phi tiêu. `turnId` hiện bị bỏ qua, DTO thiếu số thứ tự phi tiêu mong đợi; `requestId` có trường nhưng không được dùng.

TCP tự bảo đảm thứ tự byte trong một kết nối; lỗi ở đây là lệnh bị gửi lại ở tầng ứng dụng, thao tác lặp hoặc retry, không phải TCP tự giao trùng thông điệp.

**Xử lý:** dùng `(matchId, turnId, dartIndex, requestId)`; xử lý nguyên tử theo phòng. Cùng requestId trả lại kết quả cũ, không đổi state; request mới nhưng dartIndex đã xử lý bị từ chối. Client khóa thao tác khi chờ kết quả và bỏ qua event đã áp dụng.

### 2.4. P0 — Tọa độ và kích thước bàn giữa client/server không khớp

Vị trí: `DartGame_Client/src/controllers/StartGameController.java:79`, `:399`, `:636`, `:688`; `DartGame_Common/src/config/DartboardLayout.java`; `DartGame_Server/src/services/DartboardScorer.java`.

- Client gửi tọa độ tuyệt đối trong pane, tâm bàn `(225,200)`. Server tính khoảng cách tới `(0,0)`.
- **Tái hiện:** ném vào tâm nhìn thấy `(225,200)` được 0 điểm; `(0,0)` nằm ngoài bàn nhìn thấy lại được 50 điểm.
- Client vẽ bán kính 160, double 140–160, triple 80–100, bull 12/25; server dùng 170, 162–170, 99–107, 12.7/31.8. Chỉ dịch tâm vẫn chưa đủ.
- Client dùng Y tăng xuống dưới và xoay dương theo chiều kim đồng hồ; scorer lấy góc theo hệ toán học. Phải chốt phép đổi hệ và dấu góc xoay bằng kiểm tra cụ thể.
- Client cộng dồn góc cục bộ nhưng gửi góc vừa nhập, server gán góc tuyệt đối; góc thập phân còn bị ép thành int.
- Phi tiêu được thêm vào group đang xoay, có thể bị xoay thêm một lần so với tọa độ nhận được.

**Xử lý:** Common định nghĩa chung hình học, hướng trục, đơn vị và ý nghĩa góc xoay. Ví dụ giữ tọa độ bàn trong hệ màn hình: tâm gốc, Y hướng xuống, góc dương chiều kim đồng hồ; client chuyển `(paneX-centerX, paneY-centerY)`, server ánh xạ sector tương ứng. Cả hai bên dùng cùng bán kính. Góc tuyệt đối chuẩn hóa `[0,360)` do server quyết định; client áp dụng giá trị server xác nhận. Vẽ điểm trúng trong lớp phủ không xoay. Kiểm tra 20 sector, các vành và các góc 0/90/180/270 trước khi chốt.

### 2.5. P0 — Client thiếu danh tính người đang chơi và thông tin lượt

Vị trí: `GameService.java:23`, `:94`, `:155`, `:179`; `DartGame_Client/src/controllers/SocketHandler.java:125`, `:162`, `:173`; `models/GameViewState.java:24`; `controllers/StartGameController.java:613`, `:661`, `:694`.

- `MATCH_STARTED` chỉ chứa matchId; `TURN_PREPARING` chỉ có turnId; `TURN_STARTED` không có payload. PvP cũng chưa gửi `TURN_PREPARING` đầu tiên (**tái hiện**).
- Mỗi client tự đặt `currentActorName` là chính mình; `startTurn()` không cập nhật actor. Khi đổi lượt, client tiếp tục hiểu mình là người chơi.
- Server gửi `Player1`/`Player2` thay tên tài khoản (**tái hiện**), còn client so với username để cập nhật điểm. Hầu hết tài khoản sẽ bị tính là đối thủ, mất luồng ném tiếp sau phi tiêu đầu.
- Kết thúc trận gửi winnerId dạng chuỗi nhưng giao diện so với username. `loggedInUserId` ở client cũng chưa được gán từ kết quả login.
- PvE bị khởi tạo giao diện như `MatchMode.PVP`.

**Xử lý:** dùng DTO chung chứa matchId, mode, hai playerId/tên, actorId/type, turnId, phase, hai điểm và stateVersion. Login trả userId/tên thật. Dùng ID để quyết định lượt, điểm và thắng/thua; tên chỉ để hiển thị. Mỗi sự kiện trận đều phải gắn matchId để client bỏ qua sự kiện của trận cũ.

### 2.6. P1 — Kết quả và xác nhận bị gửi hai lần hoặc gửi sai

Vị trí: `DartGame_Server/src/controllers/GameController.java:16`, `:23`; `services/GameService.java:125`; `controllers/LobbyController.java:19`.

- Service đã broadcast `THROW_RESOLVED`, controller lại gửi thêm cho người ném. **Tái hiện:** người ném nhận hai bản tin, đối thủ nhận một. UI có thể tạo hai phi tiêu và hai timer chạy lượt tiếp.
- Rotation hợp lệ cũng bị gửi trùng. Rotation bị service từ chối vẫn được controller trả `ROTATION_APPLIED` cho người gửi (**tái hiện**), làm bàn trên hai máy khác nhau.
- PvE gửi `MATCH_STARTED` hai lần (**tái hiện**), tải lại màn chơi hai lần.

**Xử lý:** chỉ một tầng sở hữu phát sự kiện trận, đề xuất GameService. Controller chỉ giải mã, xác thực đầu vào và chuyển lệnh; trả lỗi nếu service từ chối. Client kiểm tra version/eventId.

### 2.7. P1 — Có thể xoay bàn giữa lượt

Vị trí: `GameService.java:39`; `StartGameController.java:636`, `:661`, `:678`.

**Tái hiện:** khi `PLAYING` và đã ném, cùng người chơi vẫn đổi góc được. Service chỉ kiểm tra userId. UI mở lại nhập góc ở `setTurn()`; `setTurnRotate()` bỏ qua người chơi được truyền vào và mở góc cho mọi client.

**Xử lý:** server chỉ cho xoay khi `WAITING_ROTATION`, đúng người có quyền, đúng turnId; một lần mỗi lượt. Giao diện bật điều khiển theo phase/actor server gửi. Cần chốt luật: người đang ném xoay bàn cho mình hay đối thủ xoay trước lượt; mã server hiện nghiêng về phương án đầu.

### 2.8. P0 — Rời trận, logout và mất kết nối không dọn trận đúng

Vị trí: `DartGame_Server/src/services/Client.java:78`; `controllers/GameController.java`; `controllers/UserController.java:21`; `services/GameService.java:158`.

- Dispatcher nhận `LEAVE_MATCH` nhưng controller không xử lý (**tái hiện**). Người rời trở về home trong khi đối thủ vẫn chờ.
- `close()` truyền matchId rỗng vào `handleDisconnect()`, nên không tìm được trận.
- Logout xóa phiên nhưng không xử lý phòng đang chơi.
- `leaveMatch()`/`handleDisconnect()` chưa kiểm tra thành viên; `finishMatch()` chưa bảo đảm chỉ kết thúc một lần và chưa đồng bộ với thao tác ném.
- Chưa có heartbeat/timeout để phát hiện mất mạng mà socket không đóng ngay.

**Xử lý:** tra `findRoomOfUser(userId)` từ phiên đã xác thực; xử lý rời trận/logout/disconnect cùng một luồng kết thúc theo phòng. Chuyển sang `FINISHED` nguyên tử một lần, dừng bot/timer, ghi kết quả, thông báo đối thủ và dọn map. Thiết kế MVP có thể xử thua khi mất kết nối được phát hiện; nếu hỗ trợ nối lại, thêm hạn chờ và snapshot state, không tự cấp lại trạng thái IDLE giữa trận.

### 2.9. P0 — Một user có thể vào nhiều phòng; phiên đăng nhập ghi đè nhau

Vị trí: `RoomManager.java:17`, `:26`, `:46`; `ClientManager.java:19`, `:27`; `UserController.java:10`.

**Tái hiện:** cùng user được tạo hai phòng; userRoomMap chỉ trỏ phòng mới. Xóa phòng cũ lại xóa mapping phòng mới. Đăng nhập hai client cùng user ghi đè map; gỡ phiên cũ làm mất cả phiên mới.

**Xử lý:** kiểm tra và giữ chỗ hai người nguyên tử lúc chấp nhận mời; từ chối tự đấu và người đang bận. `ConcurrentHashMap` riêng lẻ không làm cả chuỗi kiểm tra/tạo phòng nguyên tử. Xóa mapping có điều kiện `remove(userId, matchId)`; xóa phiên bằng `remove(userId, client)` kèm quản lý status cùng phiên. Đề xuất MVP từ chối đăng nhập trùng, và cấm đổi tài khoản trên socket đang đăng nhập.

### 2.10. P0 — Đăng nhập giả ảnh hưởng cả phiên chơi và lịch sử

Vị trí: `UserController.java:11`; `database/DBConnection.java:23`; `controllers/MatchHistoryController.java:14`.

Server chấp nhận mọi mật khẩu, lấy `abs(username.hashCode())` làm ID, không lấy từ `users`. Hai tên như `Aa` và `BB` có cùng hash nên có thể bị coi là một người. ID giả có thể không tồn tại trong DB, làm INSERT matches vi phạm khóa ngoại. Đăng ký chưa xử lý.

**Xử lý:** đăng ký/lưu mật khẩu băm, xác thực với DB, dùng khóa users.id; gửi DTO login cho client. Chặn toàn bộ lệnh lobby/game khi chưa xác thực. Đây là điều kiện để ID người chơi nhất quán giữa socket, phòng và lịch sử.

### 2.11. P1 — Bot đứng ở đầu lượt

Vị trí: `BotService.java:16`; `GameService.java:53`, `:70`, `:147`.

Bot tạo góc nhưng không gọi `applyBotRotation()`. State vẫn `WAITING_ROTATION`, cả ba lệnh ném bị từ chối. Bot còn gửi turnId 0 và userId -1 thay vì actor bot rõ ràng; -1 không phù hợp khóa ngoại player_id trong lịch sử.

**Xử lý:** dùng scheduler có tác vụ hủy được; bot chỉ chạy khi đúng match/turn/phase, thực sự áp dụng góc trước khi ném. Dùng actorType BOT và playerId null trong DB. Mỗi tác vụ kiểm tra lại version lượt; dừng khi trận đã kết thúc.

### 2.12. P1 — Ghi socket từ nhiều thread và khóa chung mọi trận

Vị trí: `Client.java:69`; `GameService.java:39`, `:65`, `:107`; `DartGame_Client/src/controllers/SocketHandler.java:246`.

Thread của hai người chơi, bot và xử lý lobby có thể cùng gọi `Client.send()` trên cùng ObjectOutputStream, chưa có cơ chế tuần tự hóa. Rủi ro hỏng stream hoặc sai thứ tự sự kiện cần kiểm thử socket đồng thời để xác nhận biểu hiện thực tế.

Đồng thời `synchronized` trên singleton GameService khóa tất cả trận với nhau; trong khóa còn gọi DB và ghi socket. Một client đọc chậm hoặc DB chậm có thể trì hoãn các phòng khác.

**Xử lý:** mỗi kết nối có hàng đợi gửi có giới hạn và một writer; đóng kết nối quá chậm theo chính sách. Mỗi phòng có hàng đợi xử lý lệnh hoặc khóa riêng, bảo đảm thứ tự chuyển state và phát event. Không giữ khóa state trong I/O mạng/DB kéo dài; tác vụ persistence phải giữ thứ tự create → throws → finish. Có thể khóa `send()` như bước sửa tối thiểu, nhưng vẫn cần giải quyết client đọc chậm.

### 2.13. P1 — Connection DB dùng chung rồi bị đóng sau thao tác

Vị trí: `DBConnection.java:84`; `MatchHistoryController.java:14`, `:37`, `:68`.

`getConnection()` trả lại cùng Connection. Các hàm lịch sử dùng try-with-resources nên đóng Connection đó sau thao tác; lần tiếp theo lấy lại Connection đã đóng. Nhiều trận còn chia sẻ cùng Connection. Exception chỉ in ra, game vẫn tiếp tục nên lịch sử có thể không lưu dù UI báo đã chơi xong.

**Xử lý:** DataSource/pool cấp connection cho từng thao tác, tự trả pool khi đóng; migration schema riêng; thêm UNIQUE `(match_id, turn_number, dart_number)` để chống lưu trùng. Chốt chính sách khi persistence lỗi: retry có kiểm soát, quan sát được lỗi, không âm thầm coi ghi DB thành công.

### 2.14. P1 — Đầu vào không hữu hạn và luật bust chưa rõ

Vị trí: `GameService.java:75`; `DartboardScorer.java:9`; `models/GameState.java:16`.

**Tái hiện:** tọa độ NaN được chấp nhận, nhận 20 điểm và tiêu hao phi tiêu. Không có kiểm tra hữu hạn ở server. Payload sai kiểu có thể gây exception rồi đóng cả phiên do catch ngoài vòng đọc.

Luật đang có: 301, ba phi tiêu/lượt, trừ điểm từng phi tiêu, về đúng 0 thì thắng; vượt điểm chỉ bỏ điểm của phi tiêu đó và tiếp tục lượt. `currentTurnScore` chưa được dùng. Chưa có quy tắc double-out hay hoàn điểm đầu lượt khi bust.

**Xử lý:** kiểm tra schema/payload, `Double.isFinite`, giới hạn dữ liệu đầu vào và góc xoay. Tọa độ hữu hạn ngoài bàn vẫn có thể tính MISS theo luật. Chọn rõ luật 301 tùy biến hay bust hoàn điểm đầu lượt; nếu cần hoàn điểm, lưu điểm đầu lượt và kết thúc lượt ngay khi bust. Không coi khác biệt luật là lỗi chắc chắn khi yêu cầu luật chưa được xác định.

### 2.15. P1 — Giao diện không dừng tác vụ khi đổi lượt/kết thúc

Vị trí: `StartGameController.java:608`, `:661`, `:712`; `SocketHandler.java:115`.

Timer ném tiếp không được lưu để hủy; đổi lượt, rời màn hình và hiện kết quả chưa có luồng dọn toàn bộ animation. Controller cũ vẫn được giữ trong Main. Listener lỗi chỉ in stacktrace rồi dừng, không báo trạng thái mất kết nối cho UI.

**Xử lý:** lưu/hủy animation và callback; mọi callback kiểm tra matchId/turnId/phase hiện tại. Chuyển màn/kết thúc phải vô hiệu thao tác và giải phóng controller cũ. Mất socket phải cập nhật trạng thái kết nối, dọn stream/socket và hiển thị lựa chọn kết nối lại phù hợp.

## 3. Luồng xử lý được đề xuất

```mermaid
sequenceDiagram
    participant A as Client A
    participant S as Server
    participant B as Client B
    A->>S: INVITE_REQUEST(targetUserId)
    S->>B: INVITE_RECEIVED(invitationId, sender, expiresAt)
    B->>S: INVITE_ACCEPT(invitationId)
    Note over S: Kiểm tra nguyên tử: đúng lời mời, cả hai online/rảnh
    S->>A: MATCH_STARTED(snapshot)
    S->>B: MATCH_STARTED(snapshot)
    S->>A: TURN_PREPARING(matchId, turnId, actorId, version)
    S->>B: TURN_PREPARING(matchId, turnId, actorId, version)
    A->>S: ROTATION_SUBMIT(matchId, turnId, angle, requestId)
    S->>A: TURN_STARTED(snapshot)
    S->>B: TURN_STARTED(snapshot)
    A->>S: THROW_COORDINATE(matchId, turnId, dartIndex, requestId, x, y)
    Note over S: Xác minh và cập nhật state một lần theo phòng
    S->>A: THROW_RESOLVED(result, bothScores, version)
    S->>B: THROW_RESOLVED(result, bothScores, version)
    Note over S: Hết 3 phi tiêu: chuẩn bị lượt B; về 0: kết thúc một lần
```

State tối thiểu: `WAITING_ROTATION → PLAYING → WAITING_ROTATION` cho lượt tiếp; từ trận đang hoạt động có thể sang `FINISHED` do thắng, bỏ cuộc hoặc mất kết nối. Lệnh không hợp lệ không được thay state. Đề xuất thêm timeout nhập góc/ném để người không thao tác không giữ trận vô hạn.

## 4. Thứ tự sửa để core ổn

1. **Chốt luật và hợp đồng Common:** hệ tọa độ, góc xoay, bust; DTO có ID, match/turn, phase, hai điểm, version và requestId. Hoàn thiện xác thực để ID nhất quán.
2. **Gia cố engine server:** đúng người/phòng/lượt, chống lặp, tính điểm chung, chuyển lượt và finish nguyên tử theo phòng. Sửa bot và vòng đời DB.
3. **Hoàn thiện multiplayer:** online, lời mời, giữ chỗ người chơi, giới hạn một phòng/user, một phiên/user; send tuần tự theo kết nối; rời trận/logout/disconnect/timeout.
4. **Nối UI với state server:** bỏ so sánh danh tính bằng tên, xử lý phase và kết quả đúng một lần, sửa hình học, hủy animation khi đổi lượt/rời trận.
5. **Kiểm thử socket và nhiều máy** theo bảng dưới trước khi coi PvP đã hoàn thành.

Không nên chỉ vá `GameController`: service, DTO, quản lý phiên/phòng và client phải cùng dùng một hợp đồng.

## 5. Hướng dẫn chạy và chơi nhiều người

### Hiện trạng hỗ trợ

Mỗi `Room` hiện có đúng hai vị trí: hai người hoặc một người và bot. “Nhiều client” phù hợp với nhiều trận 1v1 song song trên cùng server. Muốn 3+ người trong cùng trận phải đổi Room/GameState, vòng lượt và giao diện; mã hiện tại chưa hỗ trợ.

Các bước kết nối dưới đây có thể dùng để kiểm tra hạ tầng hiện tại. Luồng chơi PvP trọn vẹn ở phần tiếp theo là hành vi cần đạt sau khi sửa các lỗi trên.

### Chuẩn bị và kết nối

1. Dùng JDK 17+ và JavaFX tương thích OS/kiến trúc. JAR JavaFX trong repo hiện dành cho macOS ARM64; Windows/Linux cần bộ tương ứng. Common phải nằm trong dependency của cả Client và Server. IntelliJ `.iml` đã khai báo Common; classpath NetBeans hiện chưa khai báo module này nên cần bổ sung nếu chạy bằng NetBeans.
2. Chạy MySQL trên máy server; cấu hình URL/tài khoản tại `DartGame_Server/src/database/DBConnection.java`. Code có tạo bảng `users`, `matches`, `match_throws` nếu kết nối thành công; database `dart_socket_game` phải tồn tại. README cũ nhắc `Users/MatchHistory` không phản ánh đầy đủ schema hiện tại. Cần sửa xác thực/connection trước khi dùng lịch sử đáng tin cậy.
3. Chạy main `dartgame_server.DartGame_Server` trước. Server lắng nghe TCP **8080**.
4. Cùng một máy: mở hai tiến trình main `btl_ltm_n3.Main`, host `localhost`, hai tài khoản khác nhau. Mỗi tiến trình có kết nối riêng; không phải hai cửa sổ dùng chung SocketHandler.
5. Khác máy: cùng LAN hoặc cùng mạng VPN có định tuyến tới server. Tại `DartGame_Client/src/btl_ltm_n3/Main.java`, thay `socketHandler.connect("localhost", 8080)` bằng IP LAN/VPN của máy chạy server, rồi build lại client. Đề xuất sửa thành cấu hình `server.host/server.port` để khỏi sửa code mỗi lần.
6. Cho phép TCP 8080 vào máy server trên mạng thử nghiệm; kiểm tra kết nối bằng `nc -vz <IP-server> 8080` trên macOS/Linux hoặc `Test-NetConnection <IP-server> -Port 8080` trên PowerShell. Không cần mở MySQL cho máy client: client chỉ kết nối game server.
7. Muốn thử bốn người, mở bốn client với bốn tài khoản; tạo hai cặp đấu riêng. Xác nhận sự kiện trận A–B không đến C–D.

### Thao tác chơi dự kiến sau khi sửa

1. A/B đăng nhập và thấy nhau ở danh sách online. A chọn B đang rảnh và gửi lời mời; B chấp nhận. Hai người cùng thấy một matchId và cùng điểm 301.
2. Theo đề xuất người đang ném tự xoay: người có lượt nhập góc, bấm **Xoay**, chờ server xác nhận; đối thủ chỉ quan sát. Lượt đầu là player1 theo quy tắc server công bố trong snapshot.
3. Khi thanh X chạy, bấm **STOP X** để cố định X. Thanh Y chạy, bấm **THROW!** để cố định Y và gửi một lần ném. Chờ server tính điểm rồi mới thao tác tiếp.
4. Tối đa ba phi tiêu mỗi lượt, sau đó tự chuyển sang đối thủ. Hai màn hình phải có cùng hai điểm, góc bàn, turnId và người đang chơi.
5. Về đúng 0 thì kết thúc theo luật đã chọn. Rời trận bị xử thua; mất mạng theo chính sách timeout/nối lại đã triển khai. Hai bên cùng nhận kết quả, phòng được dọn, người còn online trở lại IDLE.

Hiện tại không thể khắc phục luồng PvP chỉ bằng đổi IP hoặc cài VPN; cần hoàn thiện xử lý ứng dụng ở các mục 2.1–2.10.

## 6. Tiêu chí nghiệm thu multiplayer

| Kịch bản | Kết quả cần đạt |
|---|---|
| Hai client cùng máy rồi khác máy | Login, online, mời, vào cùng trận và chơi hết trận |
| A–B và C–D chơi đồng thời | Điểm, chat, lượt, kết quả tách riêng; một phòng chậm không khóa phòng kia |
| Hai người cùng ném hoặc xoay sai lượt | Chỉ lệnh đúng actor/phase hợp lệ; lệnh sai không đổi state |
| C gửi lệnh vào trận A–B | Từ chối, điểm và số phi tiêu không đổi |
| Gửi lại cùng request; request lượt cũ | Không trừ điểm/tăng phi tiêu lần hai; lượt cũ bị từ chối |
| Hai lời mời được chấp nhận đồng thời, bấm bot liên tục | Mỗi user tối đa một phòng, không tạo phòng mồ côi |
| Hai client đăng nhập cùng tài khoản | Đúng chính sách một phiên; phiên cũ không xóa phiên mới |
| Một người rời, logout, tắt app hoặc ngắt mạng | Kết quả xác định một lần, đối thủ được báo, phòng/timer được dọn |
| Thắng và disconnect xảy ra gần đồng thời | Chỉ một kết quả cuối cùng, một lần ghi lịch sử |
| Tâm bàn, 20 sector, ranh giới vành, xoay 90° | Điểm khớp hình vẽ trên cả hai client |
| Điểm vượt còn lại, về đúng 0 | Tuân thủ quy tắc bust và thắng đã thống nhất |
| NaN, Infinity, payload thiếu/sai kiểu | Từ chối có kiểm soát; không sửa state |
| DB mất kết nối/khởi động lại | Không dùng Connection đã đóng; lỗi lưu có thể quan sát và phục hồi theo chính sách |
| Client đọc chậm, bot, broadcast lobby đồng thời | Không hỏng ObjectStream, không chặn các phòng khác |
| Rời màn hình khi timer đang chờ | Không tạo phi tiêu hoặc thao tác ở trận cũ |

Log kiểm thử nên gắn sessionId, userId, matchId, turnId, requestId, dartIndex và stateVersion, tránh ghi mật khẩu. So sánh snapshot của server và hai client sau mỗi chuyển trạng thái để phát hiện lệch sớm.
