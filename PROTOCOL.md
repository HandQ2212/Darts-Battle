# Darts Battle protocol v2

Client và server phải được build cùng Common. Java ObjectStream TCP; mỗi connection
có một writer tuần tự, filter deserialization và heartbeat. Không tương thích client cũ.

- Login/Register: LoginRequest -> AuthResponse(success, userId, username, message).
- ONLINE_LIST: List<OnlineUser>; chỉ gồm người đã đăng nhập, không có mật khẩu.
- INVITE_REQUEST: username; INVITE_RECEIVED: InviteResponse(id, senderId, senderName,
  expiresAt); ACCEPT/REJECT: invitationId. Invitation hết hạn sau 30 giây.
- MATCH_STARTED, TURN_PREPARING, TURN_STARTED, ROTATION_APPLIED: MatchSnapshot.
- ROTATION_SUBMIT: không còn được chấp nhận; góc xoay do server tự chọn.
- THROW_COORDINATE: ThrowCoordinateRequest(matchId, turnId, dartIndex, requestId, x, y).
- THROW_RESOLVED: ThrowResolvedResponse có actorId, requestId và snapshot sau lệnh.
- MATCH_FINISHED: MatchSnapshot với winnerId/type và endReason.
- LEAVE_MATCH: matchId. CHAT_MESSAGE: ChatMessage(matchId, text, senderName do server điền).
- ERROR: chuỗi thông báo. PING/PONG: payload null.

Kết nối Internet: chủ server cấu hình endpoint một lần trong gói cài client.
Client tự kết nối khi mở app, giữ màn đăng nhập và không yêu cầu người chơi nhập IP.
TLS được bật trong gói phát hành, client ghim chứng chỉ công khai của Mac server.
Đăng nhập/đăng ký chỉ gửi sau khi TLS handshake thành công; không hạ cấp xuống TCP
không mã hóa khi TLS lỗi. Plain TCP chỉ dành cho cấu hình phát triển được chỉ định rõ.
Chủ server cần endpoint TCP công khai/port forwarding hoặc TCP tunnel; ứng dụng
không tự vượt NAT/CGNAT chỉ bằng biết địa chỉ LAN của Mac.

Với Cloudflare Tunnel: client mở `wss://game.handq2212.site/play`, dùng các frame
binary không phân mảnh để chuyển một byte stream TLS bên trong. Bridge HTTP/WebSocket
chỉ bind 127.0.0.1:8444, chuyển byte tới TLS game server 127.0.0.1:8443. Chứng chỉ
game vẫn được ghim ở client; Cloudflare chuyển tiếp dữ liệu TLS đã mã hóa. Client
không cần cài cloudflared. Bridge giới hạn frame/queue và từ chối text/fragmented frames.

Server quyết định toàn bộ state. Một user chỉ có một phiên và một phòng; lệnh chỉ
hợp lệ khi đúng thành viên, actor, lượt và phase. Gửi lặp cùng requestId không đổi
state lần hai. Mỗi snapshot có version tăng; client bỏ event cũ/trùng và khác trận.

Luật: 301 straight-out, tối đa 3 phi tiêu/lượt. Vượt điểm: phi tiêu đó không trừ điểm,
vẫn tính một phi tiêu (giữ luật hiện tại). Đúng 0 thắng, không yêu cầu double-out.
Đầu mỗi lượt (cả HUMAN và BOT), server tự chọn góc tuyệt đối nguyên [0,359].
MATCH_STARTED/TURN_PREPARING chứa góc đích, trạng thái WAITING_ROTATION.
Client phóng bảng lên 118%, xoay ít nhất ba vòng rồi thu lại trong 2800 ms.
Server giữ WAITING_ROTATION trong thời gian này, từ chối ném sớm, sau đó gửi
ROTATION_APPLIED và TURN_STARTED với trạng thái PLAYING. Timeout ném được tính
lại từ lúc bảng dừng. Hiệu ứng không thay đổi hệ tọa độ tính điểm hoặc kích thước cửa sổ.
Tọa độ tương đối tâm bàn, X sang phải, Y xuống dưới; góc dương chiều kim đồng hồ.
Bán kính dùng chung DartboardLayout, client chuyển tọa độ pane sang tâm bàn trước gửi.
Mất kết nối, bỏ cuộc hoặc hết thời gian lượt bị xử thua; không nối lại trận cũ.
Timeout lượt mặc định 120 giây, kết nối không nhận heartbeat 45 giây.

Schema SQL giữ users/matches/match_throws; ID user thật từ DB, mật khẩu PBKDF2.
DB cấp Connection mới cho mỗi thao tác, cấu hình bằng biến môi trường.
