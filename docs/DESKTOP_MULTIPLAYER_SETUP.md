> Cập nhật v2.1: client phát hành không hỏi IP/cổng nữa. Luồng qua Internet và
> Cloudflare Tunnel nằm ở [hướng dẫn mở app và chơi](ONE_CLICK_INTERNET.md).
> Phần cấu hình kết nối thủ công bên dưới mô tả v2.0; không áp dụng cho bản cài v2.1.

# Darts Battle v2 — chạy server Mac và client Windows/macOS

## Những gì đã thay đổi

- Server quyết định điểm/lượt, xác minh thành viên phòng và thứ tự phi tiêu, chống lệnh lặp.
- PvP có danh sách online, lời mời/chấp nhận/từ chối/hết hạn; một tài khoản chỉ có một phiên và một phòng.
- PvE có bot chạy bằng scheduler; dữ liệu bot có actorType BOT và playerId null.
- Socket có writer riêng, queue giới hạn, heartbeat và timeout; xử lý theo khóa từng phòng.
- Rời trận, logout, mất kết nối và hết thời gian lượt kết thúc trận một lần.
- Client nhập/lưu IP và cổng server ở màn đăng nhập, dùng chung hình học bàn và snapshot server.
- Đăng ký/đăng nhập dùng users.id và mật khẩu PBKDF2 có salt; sửa vòng đời JDBC Connection.
- Build/đóng gói bằng Python + JDK, không phụ thuộc NetBeans hay đường dẫn JavaFX của một máy.

Đây là protocol v2: **build lại cả server và tất cả client**, không dùng client cũ.
Luật được giữ: 301, 3 phi tiêu/lượt, straight-out. Ném vượt điểm chỉ bỏ điểm phi tiêu đó,
không hoàn lại cả lượt; người đang ném chọn góc nguyên tuyệt đối 0–359 một lần trước lượt.
Timeout lượt mặc định 120 giây. Mất kết nối bị xử thua, không phục hồi trận cũ khi login lại.

## 1. Chuẩn bị máy Mac làm server

Máy Mac hiện tại đã được cài MySQL 8.4.11 qua Homebrew và tạo sẵn database/tài khoản
cho game. Chạy `./scripts/run-server.sh` từ thư mục dự án; script tự đọc cấu hình riêng
trong `.local/server.env`. Thư mục `.local` bị loại khỏi Git; không chia sẻ các file mật khẩu.

Quản trị MySQL tại máy này: `mysql --defaults-file=.local/mysql-admin.cnf`.
MySQL chỉ lắng nghe localhost:3306 và tự chạy khi đăng nhập macOS.
Quản lý dịch vụ bằng `brew services start mysql@8.4` hoặc `brew services stop mysql@8.4`.
Các bước cấu hình thủ công bên dưới dành cho máy server mới.

Máy phát triển cần JDK 17+ và Python 3. Máy chơi dùng gói đã đóng kèm Java sẽ không cần cài JDK/Python.
Server giữ MySQL như kiến trúc hiện có; client không cần MySQL và không truy cập DB trực tiếp.

Tạo database bằng tài khoản quản trị MySQL:

```sql
CREATE DATABASE IF NOT EXISTS dart_socket_game CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'darts'@'localhost' IDENTIFIED BY 'THAY_BANG_MAT_KHAU_RIENG';
GRANT ALL PRIVILEGES ON dart_socket_game.* TO 'darts'@'localhost';
```

Trong Terminal tại thư mục dự án, đặt thông tin của tài khoản vừa tạo:

```sh
export DARTS_DB_URL='jdbc:mysql://localhost:3306/dart_socket_game?connectTimeout=5000&socketTimeout=5000'
export DARTS_DB_USER='darts'
export DARTS_DB_PASSWORD='THAY_BANG_MAT_KHAU_RIENG'
python3 scripts/desktop.py run server
```

Server tự tạo bảng còn thiếu và thêm khóa duy nhất `(match_id,turn_number,dart_number)`.
Thông báo sẵn sàng là `Darts Battle server listening on 0.0.0.0:8080`.
Nếu DB không truy cập được hoặc schema không tương thích, server dừng và in nguyên nhân.
Không có mật khẩu MySQL mặc định được nhúng trong code nữa.

Sau khi đã build, chạy lại server mà không biên dịch:

```sh
java -jar build/desktop/server/input/darts-server.jar
```

Giữ thư mục `lib` nằm cạnh JAR, giữ Terminal mở khi phục vụ game. Có thể đặt `DARTS_PORT`
và `DARTS_BIND` nếu cần cổng/interface khác; mặc định TCP 8080, mọi interface.
Mac phải đang thức và tiến trình server còn chạy. Cho phép Java/server nhận kết nối trong firewall macOS.

**Database cũ:** sao lưu trước khi chạy v2. Tài khoản từ bản fake-login trước đây không phải
là tài khoản được đăng ký thật; hãy đăng ký lại qua client. Hash mật khẩu kiểu cũ không phải
PBKDF2 sẽ không đăng nhập được. Nếu bảng `users` dùng schema cũ khác `password_hash`,
cần chuyển schema hoặc dùng database mới; code không tự suy đoán/chuyển mật khẩu cũ.
Nếu dữ liệu `match_throws` đã bị trùng, startup báo lỗi khi thêm UNIQUE; kiểm tra và xử lý
bản sao dữ liệu có chủ đích, không tự xóa lịch sử để vượt qua lỗi.

## 2. Kết nối client từ máy khác

1. Mac server và máy chơi cùng LAN, hoặc cùng mạng VPN cho phép kết nối đến IP server.
2. Lấy IP LAN của Mac trong System Settings → Network; nếu dùng VPN, lấy IP của Mac trong VPN.
3. Trên client nhập IP đó vào ô **Server**, cổng **8080**. `localhost` chỉ dùng khi client chạy ngay trên Mac server.
4. Bấm đăng ký, tạo tài khoản riêng cho từng người; tên 3–32 ký tự chữ/số/_, mật khẩu 6–128 ký tự.
5. Đăng nhập. App ghi nhớ địa chỉ/cổng cho lần sau. Thay địa chỉ/cổng không cần build lại client.

Kiểm tra đường truyền nếu không kết nối được:

```sh
# macOS/Linux
nc -vz IP_CUA_MAC 8080
```

```powershell
# Windows PowerShell
Test-NetConnection IP_CUA_MAC -Port 8080
```

Nếu LAN dùng guest Wi-Fi/client isolation, các máy có thể cùng Wi-Fi nhưng không truy cập
được nhau. Chỉ mở TCP game server; không cần mở cổng MySQL cho máy chơi.
Kết nối hiện dùng TCP ObjectStream, chưa mã hóa TLS; sử dụng trong LAN tin cậy hoặc VPN,
chưa coi đây là dịch vụ để mở trực tiếp ra Internet.

## 3. Cách chơi nhiều người

- A vào danh sách đối thủ, chọn B ở trạng thái IDLE và gửi lời mời.
- B chấp nhận trong 30 giây. Hai bên vào cùng trận, bắt đầu 301 điểm; A đi trước.
- Người có lượt nhập góc 0–359 và bấm **Xoay**, chờ server xác nhận.
- Bấm **STOP X** để cố định thanh X, rồi **THROW!** để cố định Y và ném.
- Chờ kết quả trước phi tiêu tiếp theo. Hết ba phi tiêu tự đổi lượt. Về đúng 0 thắng.
- Chat chỉ đến người cùng phòng; rời trận/logout/mất kết nối/hết thời gian lượt bị xử thua.
- Một tài khoản không được đăng nhập hai máy cùng lúc. Nếu mất mạng, đợi server phát hiện
  (tối đa khoảng 45 giây không có dữ liệu) rồi đăng nhập lại.
- Bốn người có thể chơi hai trận 1v1 đồng thời. Chưa hỗ trợ hơn hai người trong một phòng.

## 4. Tạo ứng dụng desktop

Mỗi gói phải build trên đúng OS/kiến trúc của người chơi. Script chọn JavaFX theo OS
và kiến trúc của JDK; lần build đầu ở Windows/Intel Mac có thể cần Internet tải JAR từ Maven Central.
Mac ARM64 sử dụng các JAR JavaFX đã có trong repo. Runtime Java được jpackage đóng kèm.

Trên Mac:

```sh
python3 scripts/desktop.py package client --type app-image
# Tạo bộ cài kéo vào Applications:
python3 scripts/desktop.py package client --type dmg
```

Kết quả ARM64: `dist/darwin-arm64/DartsBattle.app` và `DartsBattle-2.0.0.dmg`.
Gói hiện chưa ký Developer ID/notarize; khi phân phối ngoài máy build, macOS có thể yêu cầu
người dùng cho phép mở app trong Privacy & Security. Đây chưa phải bản phát hành đã ký.

Trên Windows x64 (cài JDK 17+ x64 và Python ở máy build):

```powershell
py scripts/desktop.py package client --type app-image
```

Gửi **toàn bộ thư mục** `dist/windows-AMD64/DartsBattle`, không chỉ file EXE.
Người chơi giải nén, tạo shortcut tới `DartsBattle.exe`, mở app và nhập IP Mac server.
Gói chứa runtime, không cần IDE/JDK/Python ở máy người chơi.

Muốn bộ cài `.exe`/`.msi`, cài thêm WiX Toolset tương thích jpackage của JDK đang dùng
(JDK 17 dùng WiX 3), mở Terminal mới để PATH nhận công cụ rồi chạy:

```powershell
py scripts/desktop.py package client --type exe
# Hoặc: py scripts/desktop.py package client --type msi
```

Không thể tạo bộ cài Windows bằng jpackage trên Mac. Workflow
`.github/workflows/desktop-build.yml` đã được chuẩn bị để chạy thủ công trên GitHub Actions:
Actions → Desktop packages → Run workflow; nó chạy test, build macOS `.dmg` theo kiến trúc runner và
Windows x64 app-image ZIP. Workflow chưa được chạy/publish bởi lần sửa này.

Script không ghi đè bộ cài/app cùng tên đã có trong `dist`: đổi chỗ/xóa đúng artifact cũ
nếu chủ động muốn tạo lại. Reskin có thể làm riêng trong FXML/CSS mà không đổi luật/protocol.

## 5. Kiểm thử và giới hạn kiểm chứng

```sh
python3 scripts/desktop.py build all
python3 scripts/desktop.py test
python3 scripts/desktop.py ui-test
```

`test` tải H2 2.3.232 vào cache build, dùng database in-memory chế độ MySQL và các socket
TCP loopback thật. Có thể dùng `--h2-jar /duong/dan/h2-2.3.232.jar` để chạy offline.
Không kết nối hoặc xóa database MySQL thật. `ui-test` cần phiên đồ họa đang đăng nhập.

Kết quả trên Mac ARM64 ngày 23/10/2026: **333 kiểm tra TCP/JDBC đều qua trên cả H2 và
MySQL 8.4.11 thật; 19 kiểm tra UI smoke đều qua**. Kiểm thử MySQL dùng database riêng,
đã dọn sau kiểm tra; không dùng tài khoản/trận thử trong database game.
Entry point server khởi động/dừng thành công với MySQL thật; đã tạo Mac `.app` và `.dmg`.
Client cũng biên dịch thành công với bộ thư viện JavaFX Windows, chưa chạy native Windows.

Kiểm tra bao gồm 240 tổ hợp sector/vành/góc, đăng ký/login thật, nhiều phòng đồng thời,
chống lệnh trùng/sai lượt/sai người, payload sai, chat, bot, thắng/bust, rời/logout/disconnect,
hết hạn lời mời/lượt và JDBC sau nhiều thao tác; UI smoke test tải FXML và chạy điều khiển X/Y.

Trước khi dùng để demo nhiều máy, vẫn cần nghiệm thu ít nhất một Windows client kết nối
Mac: login → mời → chơi hết trận → rút mạng → login lại. Kiểm thử TCP loopback trên Mac
không chứng minh firewall/VPN hay thiết bị Windows thực tế.

Lịch sử phi tiêu/kết quả được ghi theo queue ngoài khóa game, thử lại tối đa ba lần.
Nếu DB hỏng kéo dài hoặc server bị kill đột ngột, lịch sử đang chờ có thể mất; log
`HISTORY_WRITE_FAILED`/`HISTORY_QUEUE_FULL` cần được xử lý. Điểm và lượt đang chơi
không phụ thuộc tốc độ ghi lịch sử. Chưa có outbox bền vững hay khôi phục trận sau server restart.

Nguồn về đóng gói: [OpenJFX](https://openjfx.io/openjfx-docs/) và
[jpackage của JDK 17](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jpackage.html).
