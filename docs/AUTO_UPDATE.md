# Tự cập nhật client

Bản client 2.5.1 kiểm tra cập nhật khi mở và có nút **Kiểm tra cập nhật** ở màn đăng nhập. Khi đồng ý, game tải gói cho đúng hệ điều hành, kiểm tra chữ ký Ed25519 của thông tin phát hành và SHA-256 của gói, rồi đóng và mở lại. Có thể hủy trong lúc tải; nếu bước cài thất bại, trình cài cố gắng khôi phục và mở lại bản cũ. Kết nối thất bại không cản trở chơi. Bản cũ chưa có chức năng này nên cần cài bản 2.5.1 một lần.

Hỗ trợ app-image `DartsBattle.app` cho Apple Silicon và thư mục app-image `DartsBattle` tạo bằng jpackage cho Windows x64. Người chơi cần quyền ghi vào thư mục chứa ứng dụng; không chạy trực tiếp trong DMG hoặc ZIP. Với Windows, giải nén toàn bộ thư mục, không tách riêng file EXE. Dữ liệu đăng nhập nằm ngoài thư mục ứng dụng.

## Bảo vệ khóa

Khóa ký riêng được tạo tại `.local/update-signing/private.pem` trên máy chủ sở hữu, không nằm trong Git và không được đưa vào client, GitHub hoặc gói phát hành. Sao lưu an toàn khóa này: mất khóa đồng nghĩa các client đã phát hành không thể xác thực bản cập nhật mới. Nếu khóa bị lộ, phải thay khóa và phát hành lại ứng dụng bằng một kênh tin cậy. Public key được nhúng trong `DartGame_Client/src/updater/update-public.der`. Các bản build Windows và Mac phải dùng cùng public key và cùng mã nguồn.

Khóa riêng hiện nằm trên Mac đã tạo nó, không tự xuất hiện khi checkout trên Windows. Có thể đem hai ZIP về Mac để ký, hoặc chuyển bản sao khóa riêng sang Windows bằng kênh an toàn và đặt vào đúng `.local/update-signing/private.pem`. Không tạo khóa mới cho mỗi bản cập nhật: client đã cài chỉ tin public key cũ.

## Quy trình phát hành

1. Với bản đầu tiên, dùng `APP_VERSION` hiện tại là `2.5.1`. Mỗi lần cập nhật sau, tăng số này trong `scripts/desktop.py` (ví dụ `2.5.2`), commit mã nguồn và public key. Kiểm tra profile server dùng để build từng nền tảng: máy này ưu tiên `.local/client-profile.properties`, máy build sạch dùng `deployment/client.properties`.
2. Trên Mac Apple Silicon, chạy `python3 scripts/desktop.py package client --type app-image` rồi `python3 scripts/update-release.py archive`. Trên Windows x64, chạy hai lệnh tương tự. Cũng có thể chạy workflow `Desktop packages` sau khi push code, lấy hai ZIP cập nhật từ artifacts của hai job. Chỉ build trên nền tảng tương ứng; không đổi tên hoặc sửa ZIP sau khi ký.
3. Đặt `DartsBattle-mac.zip` và `DartsBattle-windows.zip` vào `dist/updates/` trên máy giữ khóa riêng. Chạy `python3 scripts/update-release.py manifest` (cần OpenSSL hỗ trợ Ed25519 trong PATH). Công cụ từ chối gói sai phiên bản, kiến trúc, public key, chứng chỉ server hoặc profile so với cấu hình dự án, rồi tạo `darts-update.txt` đã ký.
4. Tạo GitHub Release công khai cho phiên bản đó tại `HandQ2212/Darts-Battle` và tải lên **cả ba file** với tên giữ nguyên: `darts-update.txt`, `DartsBattle-mac.zip`, `DartsBattle-windows.zip`. Chọn đây là latest release, không phải draft/prerelease. Sau khi tải lên, kiểm tra trên cả hai hệ điều hành bằng bản cũ. Bản chưa được công bố sẽ báo chưa thể kiểm tra, đó là trạng thái bình thường.

Client đọc `https://github.com/HandQ2212/Darts-Battle/releases/latest/download/darts-update.txt`, không dùng endpoint server trò chơi. Chữ ký được xác thực trước khi tin địa chỉ tải hoặc mã băm; chỉ cho phép HTTPS. Không đưa khóa riêng vào CI hay chia sẻ qua các artifacts.

Kiểm tra mã updater trước khi build trên Windows:

```powershell
py scripts/desktop.py updater-test
py tests/update_release_test.py
py scripts/desktop.py package client --type app-image
py scripts/update-release.py archive
```

Nếu chưa có GitHub Release công khai chứa ba file nói trên, nút kiểm tra sẽ báo máy chủ cập nhật chưa sẵn sàng. Sau khi phát hành 2.5.1 làm bản đầu, cần phát hành một phiên bản **lớn hơn** để kiểm tra toàn bộ quá trình tự cập nhật. Đây là feed công khai; GitHub Release riêng tư cần cơ chế xác thực riêng, hiện chưa được hỗ trợ.

Bản Mac hiện chỉ được ký ad-hoc cho mục đích nội bộ, chưa có Apple Developer ID/notarization. Windows cũng chưa có chứng thư ký mã. Việc phát hành rộng rãi có thể gặp cảnh báo của hệ điều hành. Trước khi phát hành cho người khác, thử cài và nâng cấp thật trên mỗi hệ điều hành. Nếu cài thất bại sau khi app đóng, thông tin chẩn đoán nằm trong `install.log` ở thư mục tạm có tiền tố `darts-update-`.
