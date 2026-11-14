# Tự cập nhật client Windows

Hiện chỉ phát hành client Windows x64; việc phát hành client Mac tạm dừng. Một release cần đúng hai file: `darts-update.txt` đã ký và `DartsBattle-windows.zip`. Không cần build hoặc cung cấp ZIP Mac để ký manifest.

Bản client 2.5.2 kiểm tra cập nhật khi mở và có nút **Kiểm tra cập nhật** ở màn đăng nhập. Khi đồng ý, game tải ZIP Windows, kiểm tra chữ ký Ed25519 của manifest và SHA-256 của ZIP, rồi đóng và mở lại. Có thể hủy trong lúc tải; nếu bước cài thất bại, trình cài cố gắng khôi phục và mở lại bản cũ. Kết nối cập nhật thất bại không cản trở chơi. Client cũ chưa có updater cần cài bản mới thủ công một lần.

## Bản chạy được hỗ trợ

Sử dụng thư mục app-image `DartsBattle` do jpackage tạo trên Windows x64, gồm `DartsBattle.exe`, `app/` và `runtime/`. Giải nén toàn bộ ZIP trước khi mở game; không tách riêng EXE. Người chơi cần quyền ghi vào thư mục chứa ứng dụng. Dữ liệu đăng nhập nằm ngoài thư mục ứng dụng.

## Khóa ký

Khóa riêng hiện có nằm tại `.local/update-signing/private.pem` trên Mac đã tạo nó. Khóa không tự xuất hiện khi pull code trên Windows và không được đưa vào Git, client, CI hoặc GitHub Release. Public key được nhúng trong `DartGame_Client/src/updater/update-public.der`; tiếp tục dùng cặp khóa này cho các bản cập nhật sau.

Có thể giữ khóa trên Mac: chỉ chuyển ZIP Windows sang Mac để ký, không cần build client Mac. Nếu cần ký hoàn toàn trên Windows, chuyển khóa hiện có bằng kênh an toàn và đặt vào đúng đường dẫn trên. Máy ký cần OpenSSL hỗ trợ Ed25519 trong PATH. Không tạo khóa mới cho mỗi release. Sao lưu khóa an toàn; mất khóa sẽ khiến các client đã phát hành không thể xác thực những bản cập nhật ký bằng khóa khác. Nếu khóa bị lộ, cần thay khóa và phân phối lại ứng dụng bằng kênh tin cậy.

## Build và tạo ZIP trên Windows

Máy build cần JDK 17+ x64 và Python 3. Với bản đầu có updater, `APP_VERSION` trong `scripts/desktop.py` là `2.5.2`. Các bản tiếp theo phải tăng phiên bản, ví dụ `2.5.3`, rồi commit mã nguồn.

Kiểm tra profile trước khi build: script ưu tiên `.local/client-profile.properties`, máy build sạch dùng `deployment/client.properties`. Gói phát hành phải dùng đúng profile trong `deployment/client.properties` và chứng chỉ `deployment/server-cert.pem`; bước ký sẽ từ chối gói không khớp. Nếu đã có `dist/windows-AMD64/DartsBattle`, đóng bản client sử dụng thư mục đó và đổi tên hoặc chuyển bản cũ sang nơi khác trước khi package.

Chạy trong thư mục dự án:

```powershell
python scripts/desktop.py updater-test
python tests/update_release_test.py
python scripts/desktop.py package client --type app-image
python scripts/update-release.py archive
```

Dừng và xử lý nếu một lệnh thất bại. `archive` nén app-image đang có, nên luôn chạy sau khi build thành công phiên bản cần phát hành.

- Bản chạy: `dist/windows-AMD64/DartsBattle/DartsBattle.exe`.
- ZIP đầy đủ để gửi người chơi và dùng cho updater: `dist/updates/DartsBattle-windows.zip`.

Thử mở bản vừa build, đăng nhập và kiểm tra nút cập nhật trên Windows thật. Các kiểm thử tự động chưa thay thế việc thử cài và nâng cấp ứng dụng thực tế.

## Build bằng GitHub Actions

Workflow **Desktop packages** chỉ chạy Windows x64 và tự chạy khi push lên nhánh `lucnvm`. Vào **Actions → Desktop packages**, mở lần chạy ứng với commit vừa push để lấy artifact. Workflow cũng hỗ trợ chạy thủ công qua **Run workflow** khi file workflow có trên nhánh mặc định.

Workflow chạy kiểm thử, package app-image và tạo archive, sau đó tải `DartsBattle-windows.zip` lên artifact **DartsBattle-windows-x64**. Tải artifact và giải nén lớp ZIP artifact của GitHub để lấy file `DartsBattle-windows.zip` bên trong. Giữ nguyên ZIP ứng dụng này khi đưa sang máy ký. Workflow không giữ private key, không ký manifest và không tự đăng GitHub Release.

## Ký manifest trên máy giữ khóa

Máy ký cần checkout cùng mã nguồn, phiên bản, public key, profile và chứng chỉ với bản Windows đã build. Đặt gói Windows cần phát hành tại `dist/updates/DartsBattle-windows.zip`, rồi chạy:

```sh
python3 scripts/update-release.py manifest
```

Nếu ký trên Windows, dùng `python` thay cho `python3`. Script chỉ yêu cầu và đưa Windows vào manifest, kể cả khi thư mục còn ZIP Mac cũ. Nó kiểm tra khóa ký khớp public key, phiên bản, thư viện Windows x64, module runtime cần cho updater, chứng chỉ server và profile; sau đó tạo `dist/updates/darts-update.txt` chứa chữ ký Ed25519 cùng phiên bản, URL và SHA-256 của ZIP Windows.

Giữ nguyên tên và nội dung ZIP sau khi ký. Nếu build hoặc sửa ZIP, phải tạo manifest mới cho ZIP đó.

## Đăng GitHub Release

Tạo release công khai tại `HandQ2212/Darts-Battle` với tag theo phiên bản (ví dụ `v2.5.2`) trỏ tới commit đã build. Tải lên đủ **hai file**, giữ nguyên tên:

```text
darts-update.txt
DartsBattle-windows.zip
```

Chọn release là **latest**, không để draft hoặc prerelease. Client đọc `https://github.com/HandQ2212/Darts-Battle/releases/latest/download/darts-update.txt`, không dùng endpoint server game. Manifest được xác thực chữ ký trước khi tin URL hoặc SHA-256; địa chỉ tải phải là HTTPS. GitHub Release riêng tư chưa được hỗ trợ.

Nếu chưa có release công khai chứa hai file trên, nút kiểm tra có thể báo máy chủ cập nhật chưa sẵn sàng. Người chơi cài 2.5.2 thủ công một lần; để kiểm chứng luồng cập nhật đầy đủ, phát hành phiên bản **lớn hơn**, mở bản cũ và thử cập nhật. Thử cả hủy tải và đường khôi phục khi cài lỗi. Chỉ xác nhận cập nhật Windows hoạt động sau khi kiểm chứng trên máy Windows thực tế.

Windows hiện chưa có chứng thư ký mã, nên hệ điều hành có thể cảnh báo khi mở bản tải về. Chữ ký manifest bảo vệ gói cập nhật của game và không thay thế chứng thư ký mã Windows. Nếu cài thất bại sau khi app đóng, xem `install.log` trong thư mục tạm có tiền tố `darts-update-`.
