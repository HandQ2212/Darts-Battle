# Kế hoạch commit và phân nhánh (27/11 – 10/12/2026)

Mỗi thành viên **tự làm và tự commit** phần việc của mình bằng tài khoản Git của chính mình, đúng ngày thực hiện. Ngày ghi dưới đây là lịch dự kiến; làm xong sớm hay muộn thì commit theo ngày thật, không chỉnh lùi ngày commit.

## 1. Mô hình nhánh

```
main ─────────────────────────────────────────────●  v2.5.4 (10/12)
  └─ develop ──●────●────●────●────●────●────●──┘
                ↑    ↑    ↑    ↑
   linh/ket-noi-xac-thuc     (Mai Linh)
   hai/sanh-cho-goi-video    (Nam Hải)
   luc/tran-dau-luat-choi    (Minh Lực)
   hieu/bot-lich-su          (Minh Hiếu)
```

- `main`: chỉ chứa bản ổn định đã phát hành. Không ai push thẳng vào main.
- `develop`: nhánh tích hợp. Tạo từ `main` ngày 27/11.
- Nhánh cá nhân: tạo từ `develop`. Khi xong một nhóm việc thì mở Pull Request (PR) vào `develop`; một thành viên khác review rồi mới merge.
- Mỗi lần trước khi mở PR: `git pull origin develop` vào nhánh của mình, chạy lại toàn bộ kiểm thử.
- Mỗi commit phải gọn một việc, tiêu đề dạng `<phân hệ>: <việc đã làm>`, ví dụ `history: định dạng thời gian trận theo giờ địa phương`.

| Vai trò | Người | Nhiệm vụ |
|---|---|---|
| Người tích hợp | Nam Hải | Tạo `develop`, merge PR, tạo PR `develop → main`, gắn tag phát hành |
| Reviewer chéo | Mai Linh ↔ Nam Hải, Minh Lực ↔ Minh Hiếu | Review PR của người kia; review xong mới được merge |
| Tác giả | Mỗi thành viên | Commit trên nhánh cá nhân, mở PR, sửa theo review |

## 2. Lịch commit chi tiết

| # | Ngày dự kiến | Người (nhánh) | Thay đổi cần commit | Tiêu đề commit gợi ý |
|---|---|---|---|---|
| 1 | 27/11 | Nam Hải (`develop`) | Tạo nhánh `develop` từ `main`, bật bảo vệ nhánh `main` (bắt buộc PR) | — (thao tác trên GitHub) |
| 2 | 27/11 | Mai Linh (`linh/ket-noi-xac-thuc`) | Cập nhật `login.puml`, `register.puml`, `logout.puml` theo giao thức hiện tại (TLS, LOGIN_RESULT, chặn đăng nhập trùng) | `uml: cập nhật sequence đăng nhập/đăng ký/đăng xuất` |
| 3 | 27/11 | Nam Hải (`hai/sanh-cho-goi-video`) | Cập nhật `invite_to_game.puml` (lời mời hết hạn 30 giây, từ chối, người đang bận) | `uml: cập nhật sequence mời đấu` |
| 4 | 28/11 | Minh Lực (`luc/tran-dau-luat-choi`) | Cập nhật `playgame_with_other_person.puml` và class diagram trận đấu theo luồng mới (server chọn góc xoay, requestId) | `uml: cập nhật sequence và class diagram trận PvP` |
| 5 | 28/11 | Minh Hiếu (`hieu/bot-lich-su`) | Cập nhật `playgame_with_bot.puml`; thêm sơ đồ ER các bảng users, matches, match_throws | `uml: sequence chơi với Bot và sơ đồ ER` |
| 6 | 29/11 | Mai Linh | Bổ sung mục kết nối vào `PROTOCOL.md`: bắt tay TLS, ghim chứng chỉ, PING/PONG mỗi 5 giây, ngắt sau 45 giây, đường WebSocket qua Cloudflare | `docs: mô tả kết nối và heartbeat trong PROTOCOL` |
| 7 | 29/11 | Nam Hải | Bổ sung vào `PROTOCOL.md` các bản tin `SESSION_*` và `MEDIA_*` (payload, chiều gửi, phản hồi) | `docs: mô tả bản tin phiên chơi và cuộc gọi` |
| 8 | 30/11 | Minh Hiếu | Sửa cột thời gian ở màn Lịch sử đấu: đang hiện chuỗi thô từ server (có thể kèm micro giây), đổi sang dạng `dd/MM/yyyy HH:mm` | `history: định dạng thời gian trận` |
| 9 | 01/12 | Minh Lực | Thêm kiểm thử biên cho tính điểm: điểm nằm đúng ranh giới vòng double/triple, bull, góc xoay 1° và 359° | `test: kiểm thử biên vùng điểm và góc xoay` |
| 10 | 01/12 | Mai Linh | Thêm kiểm thử đăng ký với tên/mật khẩu sai độ dài, đăng nhập sai mật khẩu, đăng nhập trùng phiên | `test: kiểm thử ràng buộc tài khoản` |
| 11 | 02/12 | Nam Hải | Chạy thử cuộc gọi thật giữa hai mạng khác nhau (ví dụ Wi-Fi và 4G); ghi kết quả, ảnh chụp vào `docs/TURN_SETUP.md` | `docs: kết quả thử cuộc gọi qua hai mạng` |
| 12 | 02/12 | Minh Hiếu | Thêm kiểm thử bảng xếp hạng: chỉ tính trận PvP đã kết thúc, không tính trận với Bot | `test: xếp hạng chỉ tính trận PvP` |
| 13 | 03/12 | Cả nhóm | Mở PR từ 4 nhánh cá nhân vào `develop`; reviewer chéo đọc, góp ý, tác giả sửa | `fix: sửa theo review` (nếu có) |
| 14 | 04/12 | Nam Hải | Merge 4 PR vào `develop`, chạy toàn bộ kiểm thử trên `develop` | — (merge commit) |
| 15 | 05/12 | Minh Lực | Thử chơi PvP thật trên hai máy qua Internet; nếu gặp lỗi đồng bộ hoặc hiển thị thì sửa trên nhánh mình và mở PR mới | `match: sửa <lỗi tìm được>` |
| 16 | 05/12 | Mai Linh | Thử rớt mạng giữa trận (tắt Wi-Fi 10 giây, 60 giây); ghi lại hành vi tự kết nối lại; sửa nếu sai | `network: <sửa nếu có>` |
| 17 | 06/12 | Minh Hiếu | Cập nhật số phiên bản 2.5.4, viết ghi chú phát hành | `release: chuẩn bị 2.5.4` |
| 18 | 08/12 | Cả nhóm | Mỗi người hoàn thiện phần báo cáo cá nhân (use case chi tiết, class, sequence, ER) từ các sơ đồ đã commit ở mục 2–5 | — (file báo cáo) |
| 19 | 09/12 | Nam Hải | PR `develop → main`; cả nhóm review lần cuối | — |
| 20 | 10/12 | Nam Hải | Merge vào `main`, gắn tag `v2.5.4`; GitHub Actions build bản Windows | — (tag) |

## 3. Lệnh mẫu cho mỗi thành viên

Lần đầu (sau khi `develop` đã có):

```bash
git fetch origin
git switch -c linh/ket-noi-xac-thuc origin/develop   # đổi tên nhánh theo người
```

Mỗi lần làm xong một việc:

```bash
git add <các file đã sửa>
git commit -m "test: kiểm thử ràng buộc tài khoản"
git push -u origin linh/ket-noi-xac-thuc
```

Kiểm tra máy đang commit đúng tên mình trước khi bắt đầu:

```bash
git config user.name
git config user.email
```
