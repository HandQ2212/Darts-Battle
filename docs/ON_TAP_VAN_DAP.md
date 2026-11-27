# Ôn tập vấn đáp: Hệ thống mạng của Darts Battle

---

## 0. Tóm tắt nhanh: Server làm gì, Client làm gì, kết nối kiểu gì

### 0.1. Bức tranh tổng thể

```text
┌────────────┐                       ┌────────────────────────┐
│ Client A   │ ◄──── TCP + TLS ────► │ DARTS BATTLE SERVER    │              ┌──────────────┐
│ (JavaFX)   │    gửi/nhận Message   │                        │              │ MySQL        │
└────────────┘                       │ mỗi client = 1 thread  │              │              │
                                     │                        │ ◄── JDBC ──► │ users        │
┌────────────┐                       │ giữ trạng thái, tính   │              │ matches      │
│ Client B   │ ◄──── TCP + TLS ────► │ điểm, quyết thắng thua │              │ match_throws │
│ (JavaFX)   │    gửi/nhận Message   │                        │              └──────────────┘
└────────────┘                       └────────────────────────┘

  Cả Client và Server cùng dùng module DartGame_Common
  (Message, MessageType, DTO, WireFilter, TransportSecurity)
```

| | Vai trò | Ví von |
|---|---|---|
| **Server** | Giữ toàn bộ trạng thái, tính điểm, quyết định thắng thua, lưu DB | Trọng tài |
| **Client** | Hiển thị giao diện, gửi thao tác của người chơi, vẽ lại kết quả server gửi về | Màn hình và tay cầm |
| **Kết nối** | TCP giữ mở suốt phiên chơi, trao đổi Java Object đã serialize, bọc TLS | Đường dây điện thoại luôn mở |

**Câu tóm tắt để trả lời thầy:** "Hệ thống dùng mô hình client–server trên TCP, kết nối giữ mở suốt phiên, trao đổi Java Object có serialize. Server xử lý toàn bộ logic theo kiểu mỗi client một thread và đồng bộ trạng thái cho các người chơi. Client chỉ gửi thao tác và hiển thị kết quả."

### 0.2. Kết nối kiểu gì?

```text
 CLIENT (SocketHandler)                                SERVER
            │                                             │
            │                                             │ ServerSocket.bind(0.0.0.0:port)
            │                                             │ accept()  ← đứng chờ client
            │                                             │
            │─ 1. SYN ───────────────────────────────────►│   ┐
            │◄────────────────────────────────── SYN-ACK ─│   │ TCP bắt tay 3 bước
            │─ ACK ──────────────────────────────────────►│   ┘
            │                                             │ accept() trả về Socket
            │                                             │ new Client(socket).start()  → 1 thread mới
            │                                             │
            │◄═ 2. TLS handshake ════════════════════════►│   client so khớp chứng chỉ đã ghim
            │                                             │
            │─ 3. header ObjectOutputStream ─────────────►│   Cả 2 bên tạo ObjectOutputStream + flush() TRƯỚC
            │◄──────────────── header ObjectOutputStream ─│   4. Sau đó mới tạo ObjectInputStream (tránh deadlock)
            │                                             │
            │─ 5. LOGIN_REQUEST(username, password) ─────►│   UserController kiểm tra MySQL
            │◄─────────────── LOGIN_RESULT(AuthResponse) ─│
            │                                             │
┄┄ lặp lại mỗi 5 giây ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
            │─ 6. PING ──────────────────────────────────►│
            │◄───────────────────────────────────── PONG ─│   45 giây không nhận gì → coi như rớt mạng, đóng
            │                                             │
```

- **TCP** chứ không phải UDP: game theo lượt cần dữ liệu đến đủ và đúng thứ tự.
- **Kết nối giữ mở suốt phiên** (khác HTTP hỏi–đáp xong là đóng). Nhờ vậy **server tự đẩy dữ liệu xuống client** bất cứ lúc nào: có lời mời, đối thủ vừa ném…
- **Định dạng:** object `Message { type, payload }`, gửi bằng `writeObject`, nhận bằng `readObject`.

### 0.3. Các tầng giao thức (từ trên xuống)

```text
┌─────────────────────────────────────────────────────────────────────┐
│ Message (Java Object)                 ← giao thức của game          │
│ ObjectOutputStream/ObjectInputStream  ← serialize object thành byte │
│ TLS 1.2 / 1.3                         ← mã hoá, ghim chứng chỉ      │
│ WebSocket (binary)                    ← CHỈ khi đi qua Cloudflare   │
│ TCP                                   ← tin cậy, đúng thứ tự        │
│ IP                                                                  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 1. Kiến trúc 3 module

```text
┌───────────────────┐             ┌─────────────────────────────────────┐             ┌──────────────────────┐
│ DartGame_Client   │             │ DartGame_Common                     │             │ DartGame_Server      │
│                   │             │                                     │             │                      │
│ views (FXML, CSS) │ ── dùng ──► │ protocol: Message, MessageType, DTO │ ◄── dùng ── │ DartGame_Server.main │
│ controllers       │             │ protocol: WireFilter                │             │ controllers          │
│ SocketHandler     │             │ network: TransportSecurity          │             │ services             │
└───────────────────┘             │ enums, config                       │             │ database             │
                                  └─────────────────────────────────────┘             └──────────────────────┘
```

- **Server có toàn quyền (authoritative server).** Client chỉ gửi "ý định" (ví dụ ném vào toạ độ x,y). Việc tính điểm, đổi lượt, xử thắng thua đều do server làm rồi gửi kết quả lại. Nhờ vậy client không gian lận được.
- **DartGame_Common** chứa phần cả hai bên phải giống hệt nhau. Client và server phải build cùng bản Common, nếu không thì deserialize sẽ lỗi.

---

## 2. Giao thức: gửi cái gì qua mạng?

**Đơn vị truyền là một object `Message`** (`DartGame_Common/src/protocol/Message.java`):

```java
class Message implements Serializable { MessageType type; String requestId; Object payload; }
```

```text
┌──────────────────────────────────┐
│ Object Message                   │                      ┌────────────┐                                  ┌────────────────┐
│ type    = THROW_COORDINATE       │ ── writeObject() ──► │ chuỗi byte │ ── readObject() + WireFilter ──► │ Object Message │
│ payload = ThrowCoordinateRequest │                      │ trên TCP   │                                  │ ở bên kia      │
└──────────────────────────────────┘                      └────────────┘                                  └────────────────┘
```

- **Serialization**: Java tự đóng gói object thành byte và tự tách từng bản tin, nên không cần tự định nghĩa độ dài hay ký tự phân cách.
- `MessageType` là enum có khoảng 45 loại lệnh, gồm 2 nhóm:
  - **Request** (client gửi lên): `LOGIN_REQUEST`, `INVITE_REQUEST`, `THROW_COORDINATE`…
  - **Response/Event** (server gửi xuống): `LOGIN_RESULT`, `MATCH_STARTED`, `THROW_RESOLVED`…
- Sau mỗi lần ghi, code gọi `out.reset()` để xoá cache tham chiếu của ObjectStream. Nếu không reset, khi gửi lại cùng một object đã bị sửa, bên kia vẫn nhận bản cũ.
- **`WireFilter`** chỉ cho phép deserialize các class thuộc `protocol.*`, `enums.*` và vài kiểu cơ bản (String, Long, ArrayList…). Mục đích là **chống tấn công deserialization**.

---

## 3. Server làm gì?

### 3.1. Sơ đồ xử lý bên trong server

```text
DartGame_Server.main:  while(true) new Client(server.accept()).start();
        │
        │  mỗi kết nối mới = 1 thread Client
        ▼
┌─────────────────────────────────────────────────┐
│ Thread Client  (services/Client.java)           │
│ readObject() → handleMessage(msg): xem msg.type │
└─────────────────────────────────────────────────┘
        │
        ├─ PING                      ──► trả PONG ngay
        ├─ LOGIN / REGISTER / LOGOUT ──► UserController ──► MySQL (bảng users), ClientManager
        ├─ ONLINE_LIST, INVITE_*,    ──► LobbyService ──► ClientManager, RoomManager
        │      CREATE_BOT_MATCH, CHAT
        ├─ THROW_COORDINATE,         ──► GameService ──► RoomManager, BotService, HistoryWriter ──► MySQL
        │      LEAVE_MATCH
        ├─ GET_RANKING, GET_HISTORY  ──► RankingController / HistoryQueryService ──► MySQL
        └─ MEDIA_*                   ──► MediaSignalingService (chuyển tín hiệu gọi thoại)

Thread maintenance (chạy mỗi 1 giây): huỷ lời mời quá 30s, xử thua người hết 120s
```

### 3.2. Danh sách việc server làm

1. **Lắng nghe và nhận kết nối:** `ServerSocket.accept()`, mỗi client mới thì tạo một thread `Client` riêng (**thread-per-client**).
2. **Nhận lệnh và phân loại (router):** đọc `Message.type` rồi chuyển tới controller tương ứng.
3. **Xác thực:** đăng nhập/đăng ký, đối chiếu bảng `users` trong MySQL, mật khẩu hash PBKDF2.
4. **Quản lý người online:** `ClientManager` (ai online, đang rảnh hay bận) và broadcast danh sách cho mọi người.
5. **Ghép trận:** nhận lời mời, chuyển lời mời, tạo phòng khi đồng ý; hoặc tạo trận với Bot.
6. **Chạy luật chơi:** chọn góc xoay bảng, **tính điểm từ toạ độ (x,y)**, trừ điểm luật 301, đổi lượt, xử thắng/thua.
7. **Điều khiển Bot:** bot ném ngay trên server.
8. **Đồng bộ:** gửi kết quả mỗi thay đổi cho **cả 2 người chơi**.
9. **Lưu DB:** lịch sử trận, từng lượt ném, bảng xếp hạng.
10. **Chuyển tin:** chat, tín hiệu khởi tạo cuộc gọi WebRTC.

### 3.3. Các bước khởi động (`DartGame_Server.java`)

1. `DBConnection.initialize()` tự tạo bảng `users`, `matches`, `match_throws` nếu chưa có.
2. Tạo `ServerSocket`: không có keystore thì dùng TCP thường (dev, cổng 8080), có keystore thì tạo `SSLServerSocket` (TLS).
3. `bind(0.0.0.0:port)`.
4. Vòng lặp chính: `while(true) new Client(server.accept()).start();`
5. Bật thread `maintenance` chạy mỗi giây.

---

## 4. Client làm gì?

### 4.1. Sơ đồ bên trong client

```text
CHIỀU GỬI ĐI                                       CHIỀU NHẬN VỀ

┌─────────────────────────────┐                    ┌─────────────────────────────┐
│ Người chơi bấm / nhập       │                    │ SERVER                      │
└─────────────────────────────┘                    └─────────────────────────────┘
               │                                                  ║   TCP
               ▼                                                  ▼
┌─────────────────────────────┐                    ┌─────────────────────────────┐
│ Màn hình FXML + Controller  │                    │ Thread reader               │
│ (LoginController,           │                    │ readObject() từ socket      │
│  StartGameController...)    │                    └─────────────────────────────┘
└─────────────────────────────┘                                   │   Platform.runLater()
               │   login(), inviteToPlay()...                     ▼
               ▼                                   ┌─────────────────────────────┐
┌─────────────────────────────┐                    │ receive(msg):               │
│ SocketHandler.sendData(msg) │                    │ switch theo msg.type        │
│ đẩy vào hàng đợi gửi        │                    └─────────────────────────────┘
└─────────────────────────────┘                                   │
               │                                                  ▼
               ▼                                   ┌─────────────────────────────┐
┌─────────────────────────────┐                    │ GameViewState / đổi màn     │
│ Thread writer               │                    │ (bản sao trạng thái để vẽ)  │
│ writeObject() ra socket     │                    └─────────────────────────────┘
└─────────────────────────────┘                                   │
               ║   TCP                                            ▼
               ▼                                   ┌─────────────────────────────┐
┌─────────────────────────────┐                    │ Người chơi thấy kết quả     │
│ SERVER                      │                    └─────────────────────────────┘
└─────────────────────────────┘

Thread heartbeat (mỗi 5 giây): gửi PING, nếu đang mất kết nối thì tự kết nối lại
```

### 4.2. Danh sách việc client làm

1. **Tự kết nối tới server** khi mở app, địa chỉ lấy từ `server.properties` (người chơi không nhập IP).
2. **Hiển thị giao diện JavaFX:** đăng nhập, sảnh, danh sách online, bàn phi tiêu, xếp hạng, lịch sử.
3. **Gửi yêu cầu:** đăng nhập, mời đấu, chấp nhận/từ chối, **toạ độ phi tiêu được click**, rời trận, chat.
4. **Nhận sự kiện từ server và cập nhật giao diện**, luôn qua `Platform.runLater` (chỉ UI thread được sửa giao diện).
5. **Gửi PING mỗi 5s**, mất kết nối thì **tự kết nối lại**.
6. **Không tự tính điểm, không tự quyết thắng thua.**

---

## 5. Mô hình luồng (thread): phần quan trọng nhất

Mỗi kết nối có 2 thread ở mỗi phía: một thread chỉ đọc, một thread chỉ ghi.

```text
CHIỀU CLIENT → SERVER                         CHIỀU SERVER → CLIENT

┌──────────────────────────────────┐          ┌──────────────────────────────────┐
│ CLIENT: JavaFX UI thread         │          │ SERVER: thread Client, thread    │
└──────────────────────────────────┘          │ đối thủ, thread bot, hẹn giờ     │
                  │   sendData() → offer()    └──────────────────────────────────┘
                  ▼                                             │   send() → offer()
┌──────────────────────────────────┐                            ▼
│ CLIENT: hàng đợi gửi (128)       │          ┌──────────────────────────────────┐
└──────────────────────────────────┘          │ SERVER: hàng đợi gửi (256)       │
                  │   take()                  └──────────────────────────────────┘
                  ▼                                             │   take()
┌──────────────────────────────────┐                            ▼
│ CLIENT: thread writer            │          ┌──────────────────────────────────┐
│ writeObject()                    │          │ SERVER: thread client-writer     │
└──────────────────────────────────┘          │ writeObject()                    │
                  ║   TCP                     └──────────────────────────────────┘
                  ▼                                             ║   TCP
┌──────────────────────────────────┐                            ▼
│ SERVER: thread Client (reader)   │          ┌──────────────────────────────────┐
│ readObject()                     │          │ CLIENT: thread reader            │
└──────────────────────────────────┘          │ readObject()                     │
                  │                           └──────────────────────────────────┘
                  ▼                                             │   Platform.runLater()
┌──────────────────────────────────┐                            ▼
│ SERVER: handleMessage()          │          ┌──────────────────────────────────┐
└──────────────────────────────────┘          │ CLIENT: JavaFX UI thread         │
                                              │ cập nhật màn hình                │
                                              └──────────────────────────────────┘
```

| | Server (`services/Client.java`) | Client (`SocketHandler`) |
|---|---|---|
| Reader | Thread `Client` lặp `in.readObject()` rồi gọi `handleMessage()` | `listen()` đọc message rồi chuyển vào `Platform.runLater(receive)` |
| Writer | Thread `client-writer` lấy message từ `BlockingQueue` và ghi ra socket | `write()` lấy từ queue và ghi ra socket |

**Vì sao cần hàng đợi và thread ghi riêng?**

- Nhiều thread cùng muốn gửi cho một client (thread đối thủ, bot, hẹn giờ). Nếu cùng gọi `writeObject` thì byte bị trộn lẫn. Đẩy hết vào queue để **một writer duy nhất ghi tuần tự**.
- `send()` chỉ `offer()` vào queue nên **không bị chặn**. Một client mạng chậm không làm cả server chậm theo. Queue đầy (256) thì server ngắt client đó.

**Vì sao tạo `ObjectOutputStream` trước?** Constructor của `ObjectInputStream` dừng chờ đọc header từ bên kia. Nếu cả hai bên cùng tạo Input trước thì chờ nhau mãi, tức là **deadlock**.

---

## 6. Các luồng nghiệp vụ chính

### a) Đăng nhập

```text
 CLIENT                                    SERVER                                     MySQL
    │                                         │                                         │
    │─ LOGIN_REQUEST(username, password) ────►│                                         │
    │                                         │─ SELECT ... WHERE username = ? ────────►│
    │                                         │◄──────────────────── id, password_hash ─│
    │                                         │ verify mật khẩu (PBKDF2)                │
    │                                         │ ClientManager.addClient                 │
    │                                         │ (mỗi tài khoản 1 phiên)                 │
    │◄───────────── LOGIN_RESULT(ok, userId) ─│                                         │
    │◄────────── ONLINE_LIST (gửi mọi người) ─│                                         │
    │─ FEATURE_SUPPORT, ONLINE_LIST ─────────►│                                         │
    │                                         │                                         │
```

Truy vấn dùng `PreparedStatement` để chống SQL injection. Mật khẩu chỉ lưu dạng hash.

### b) Mời đấu PvP

```text
       CLIENT A                          SERVER (LobbyService)                         CLIENT B
           │                                       │                                       │
           │─ INVITE_REQUEST("B") ────────────────►│                                       │
           │                                       │ A, B rảnh? tạo lời mời 30s            │
           │                                       │ đặt A, B = INVITED                    │
           │                                       │─ INVITE_RECEIVED(id, "A") ───────────►│
           │◄──────────── INVITE_RESULT("Đã gửi") ─│                                       │
┄┄ Trường hợp 1: B đồng ý ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
           │                                       │◄────────────────── INVITE_ACCEPT(id) ─│
           │                                       │ createPvpRoom + startMatch            │
           │◄────────────────────── MATCH_STARTED ─│                                       │
           │                                       │─ MATCH_STARTED ──────────────────────►│
┄┄ Trường hợp 2: B từ chối hoặc hết 30 giây ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
           │                                       │◄────────────────── INVITE_REJECT(id) ─│
           │◄──────── INVITE_RESULT("bị từ chối") ─│                                       │
           │                                       │─ INVITE_RESULT ──────────────────────►│
           │                                       │                                       │
```

### c) Một lượt ném (trọng tâm)

```text
  CLIENT A (tới lượt)                    SERVER (GameService)                          CLIENT B
           │                                       │                                       │
           │◄─────────── TURN_PREPARING(góc xoay) ─│                                       │
           │                                       │─ TURN_PREPARING(góc xoay) ───────────►│
           │ bảng xoay 2,8 giây                    │ chặn mọi lệnh ném sớm                 │ bảng xoay 2,8 giây
           │◄──── ROTATION_APPLIED + TURN_STARTED ─│                                       │
           │                                       │─ ROTATION_APPLIED + TURN_STARTED ────►│
           │                                       │                                       │
           │─ THROW_COORDINATE(x, y, requestId) ──►│                                       │
           │                                       │ synchronized(room) {                  │
           │                                       │   đúng lượt? đúng phase?              │
           │                                       │   tính điểm, trừ điểm (301)           │
           │                                       │ }                                     │
           │◄────── THROW_RESOLVED(điểm, còn lại) ─│                                       │
           │                                       │─ THROW_RESOLVED(điểm, còn lại) ──────►│
┄┄ Nếu đã ném đủ 3 phi tiêu ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
           │◄──────────── TURN_PREPARING (lượt B) ─│                                       │
           │                                       │─ TURN_PREPARING (lượt B) ────────────►│
┄┄ Nếu về đúng 0 điểm ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
           │◄─────────── MATCH_FINISHED (A thắng) ─│                                       │
           │                                       │─ MATCH_FINISHED (A thắng) ───────────►│
           │                                       │                                       │
```

Cách tính điểm: `DartboardScorer` lấy khoảng cách từ tâm để biết vùng (bull, single, double, triple), lấy góc bằng `atan2(y, x)` trừ đi góc xoay để biết ô số 1..20. Luật 301: vượt điểm (bust) thì không trừ, về đúng 0 là thắng.

### d) Trạng thái của một trận

```text
MATCH_STARTED
     │
     ▼
WAITING_ROTATION ──(sau 2,8 giây)──► PLAYING ──(đủ 3 phi tiêu, đổi lượt)──► WAITING_ROTATION ...
     │                                 │
     │                                 ├──(ném phi tiêu 1, 2)──► vẫn PLAYING
     │                                 │
     │                                 └──(về 0 điểm / bỏ cuộc / mất kết nối / hết 120s)──► FINISHED
     │
     └──(bỏ cuộc / mất kết nối)──► FINISHED
```

### e) Chơi với Bot (PvE)

```text
       CLIENT A                          SERVER (GameService)                    BotService (ở server)
           │                                       │                                       │
           │◄──────────── TURN_STARTED (lượt BOT) ─│                                       │
           │                                       │─ playTurn() ─────────────────────────►│
┄┄ lặp 3 lần, mỗi lần cách nhau 700ms ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
           │                                       │◄────────────────── playBotStep(x, y) ─│
           │                                       │ resolveThrow() như người thật         │ BotAim chọn x, y theo độ khó
           │◄───────────────────── THROW_RESOLVED ─│                                       │
┄┄ hết 3 phi tiêu ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
           │◄──────────── TURN_PREPARING (lượt A) ─│                                       │
           │                                       │                                       │
```

### f) Mất kết nối

```text
 CLIENT A                                 SERVER                                 CLIENT B
     │                                       │                                       │
     │╌ rớt mạng, hết PING ╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌x│                                       │
     │                                       │ 45 giây không nhận được gì:           │
     │                                       │ SocketTimeoutException                │
     │                                       │ close() → logout(DISCONNECT)          │
     │                                       │─ MATCH_FINISHED (B thắng) ───────────►│
     │                                       │─ ONLINE_LIST (A offline) ────────────►│
     │ A về màn đăng nhập,                   │                                       │
     │ heartbeat tự kết nối lại              │                                       │
     │                                       │                                       │
```

Khi trận kết thúc (bất kể lý do), `HistoryWriter` ghi kết quả vào DB trên một thread riêng, thử lại 3 lần, để DB chậm **không chặn** trận đấu.

---

## 7. Đồng bộ và tính nhất quán (thầy hay hỏi)

```text
Thread người chơi A ──┐       ┌──────────────────────────┐       ┌────────────────────────────┐
Thread người chơi B ──┤       │ synchronized(room)       │       │ Trạng thái trận            │
Thread Bot ───────────┼ ────► │ mỗi lúc chỉ 1 thread vào │ ────► │ điểm, lượt, phase, version │
Thread hẹn giờ ───────┘       └──────────────────────────┘       └────────────────────────────┘
```

- **`synchronized(room)`**: mọi thay đổi trạng thái của một trận chạy trong monitor của phòng đó. Các phòng khác nhau vẫn chạy song song.
- **`ConcurrentHashMap`** cho các danh sách dùng chung (client online, phòng).
- **Idempotent bằng `requestId`**: mỗi lần ném có một UUID. Gửi lại cùng requestId thì server trả kết quả cũ, không trừ điểm lần hai.
- **`version` trong snapshot**: mỗi thay đổi tăng version. Client bỏ qua event cũ, trùng, hoặc của trận khác.
- **`turnId`, `dartIndex`**: chặn ném sai lượt hoặc ném "từ quá khứ".

---

## 8. Heartbeat, timeout, mất kết nối

- Cứ **5s** client gửi `PING`, server trả `PONG`.
- Server đặt `setSoTimeout(45000)`: quá 45s không nhận được gì thì `readObject` ném `SocketTimeoutException`, server đóng kết nối.
- `Client.close()` gọi `LobbyService.logout(…, "DISCONNECT")`: huỷ lời mời, xử **thua** trận đang chơi, gỡ khỏi danh sách online, broadcast danh sách mới.
- Phía client, khi đọc/ghi lỗi thì `failed()` đưa về màn đăng nhập, heartbeat **tự kết nối lại**. Trận cũ không nối lại.

---

## 9. Bảo mật đường truyền và chạy qua Internet

```text
┌─────────────────────────────────┐
│ Client                          │
└─────────────────────────────────┘
                 │   wss://game-backsite.../play  (WebSocket, cổng 443)
                 ▼
┌─────────────────────────────────┐
│ Cloudflare                      │
└─────────────────────────────────┘
                 │   Cloudflare Tunnel
                 ▼
┌─────────────────────────────────┐
│ WebSocketBridge  127.0.0.1:8444 │
└─────────────────────────────────┘
                 │   TCP nội bộ
                 ▼
┌─────────────────────────────────┐
│ TLS Game Server  127.0.0.1:8443 │
└─────────────────────────────────┘

TLS mã hoá từ Client tới Game Server: Cloudflare chỉ thấy byte đã mã hoá.
```

- **TLS** (TLSv1.2/1.3) với **certificate pinning**: client nhúng sẵn chứng chỉ server và so sánh từng byte (`TransportSecurity.pinnedContext`). Server giả mạo bị từ chối. Không bao giờ chuyển xuống TCP thường khi TLS lỗi.
- **Cloudflare Tunnel** dùng vì máy chủ ở nhà nằm sau NAT. `WebSocketSocket` giả lập một `Socket` trên WebSocket, nên TLS và ObjectStream bên trong **không cần sửa gì** (phân tầng giao thức, tunneling).
- **Gọi thoại/video**: server chỉ làm **signaling server** (chuyển SDP, ICE candidate). Âm thanh/hình ảnh đi **P2P qua WebRTC**, hoặc qua TURN khi P2P không được.

```text
┌──────────┐                                           ┌───────────────────┐                    ┌──────────┐
│ Client A │ ── MEDIA_DESCRIPTION, MEDIA_CANDIDATE ──► │ Server            │ ── chuyển tiếp ──► │ Client B │
└──────────┘                                           │ (chỉ chuyển tiếp) │                    └──────────┘
                                                       └───────────────────┘

  Client A ◄═══════ âm thanh / hình ảnh đi thẳng P2P (WebRTC) ═══════► Client B
```

---

## 10. Câu hỏi vấn đáp hay gặp và gợi ý trả lời

| Câu hỏi | Ý chính |
|---|---|
| Vì sao dùng TCP mà không dùng UDP? | Cần tin cậy và đúng thứ tự; game theo lượt nên không cần độ trễ cực thấp. |
| Server phục vụ nhiều client cùng lúc thế nào? | Thread-per-client: `accept()` xong tạo một `Client extends Thread`. Trạng thái dùng chung để trong ConcurrentHashMap và singleton. |
| Hai người cùng gửi lệnh một lúc thì sao? | `synchronized(room)` cùng với kiểm tra lượt/phase/turnId. |
| Làm sao server gửi chủ động cho client (push)? | Server giữ socket của mọi client trong `ClientManager`, gọi `sendToUser(id, msg)` hoặc `broadcast`. Kết nối lâu dài, không phải request–response như HTTP. |
| Làm sao biết client rớt mạng? | Heartbeat PING/PONG kết hợp `SO_TIMEOUT` 45s, cộng thêm exception khi đọc/ghi. |
| Vì sao tạo ObjectOutputStream trước? | Tránh deadlock do constructor ObjectInputStream chờ đọc header. |
| Vì sao cần `out.reset()`? | Xoá cache tham chiếu, để object đã sửa được gửi lại đúng giá trị mới. |
| Chống gian lận? | Server tính toàn bộ điểm; client chỉ gửi toạ độ, toạ độ được kiểm tra hợp lệ. |
| Bảo mật? | TLS + certificate pinning, PBKDF2 hash mật khẩu, PreparedStatement, WireFilter, mỗi tài khoản một phiên. |
| Giao diện có bị đơ khi chờ mạng không? | Không. Mạng chạy ở thread riêng, UI cập nhật qua `Platform.runLater`. |

---

## 11. File nên mở ra xem trước khi đi thi

- `DartGame_Server/src/dartgame_server/DartGame_Server.java`: vòng `accept()`
- `DartGame_Server/src/services/Client.java`: reader/writer/router phía server
- `DartGame_Client/src/controllers/SocketHandler.java`: kết nối, đọc/ghi, xử lý message phía client
- `DartGame_Server/src/services/GameService.java`: `submitThrow`, `resolveThrow`, `switchTurn`
- `DartGame_Server/src/services/LobbyService.java`: đăng nhập, mời đấu
- `DartGame_Common/src/protocol/`: `Message`, `MessageType`, `WireFilter`
- `PROTOCOL.md`: mô tả giao thức
