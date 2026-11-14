# Chẩn đoán mất kết nối

Client ghi log tại `%USERPROFILE%\DartsBattle\logs`; server ghi tại
`.local/logs`. Có thể đổi thư mục bằng thuộc tính Java `-Ddarts.logs.dir`.
Mỗi phía giữ tối đa bốn file, mỗi file khoảng 1 MB. Thời gian có múi giờ.
Tìm sự kiện `connection_lost`, `read_error` hoặc `write_error` của
client, rồi đối chiếu `userId` và thời gian với server. Các sự kiện bridge
và server liên hệ qua `backendPort`. Không có mã kết nối chung giữa client
và server; dùng thời gian và ID người dùng để đối chiếu.

Trên máy chạy tunnel, mở Event Viewer → Windows Logs → System, lọc nguồn
Service Control Manager và tìm Cloudflared theo thời gian tương ứng. Application
cũng có thể chứa sự kiện nguồn Cloudflared. Khi chia sẻ, chỉ xuất các sự kiện
cần thiết và kiểm tra nội dung trước khi gửi; không gửi token hoặc file cấu hình.
Log game không ghi mật khẩu, token, nội dung chat, SDP hoặc dữ liệu media.

Từ bản 2.4.1, client chỉ yêu cầu thêm dữ liệu WebSocket sau khi đã lấy gói
khỏi hàng đợi. Client/server gom các lần ghi nhỏ và flush sau mỗi bản tin.
Bridge chờ hàng đợi gửi giảm khi có áp lực; nếu chờ quá 8 giây sẽ ghi
`bridge_output_timeout`. Các giới hạn bất thường có sự kiện
`websocket_queue_full`, `websocket_frame_limit` hoặc `bridge_input_queue_full`.
Giữ các log này khi báo lỗi; không kết luận mọi mã đóng 1006 đều do Cloudflare.
