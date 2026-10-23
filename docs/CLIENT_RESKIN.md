# Giao diện client

Giao diện casual 2D theo hướng rừng hoạt hình, nút cam bo tròn, bảng kem và viền gỗ.
Style dùng chung nằm ở `DartGame_Client/src/views/theme.css`; các màn FXML nạp
stylesheet này trực tiếp để hiển thị nhất quán cả khi chạy trong IDE và bộ cài.

`ui/MenuArtwork.java` và `ui/ArenaBackdrop.java` trình bày trang trí 2D bằng Canvas,
chỉ vẽ lại khi đổi kích thước và không nhận input. Nền rừng là ảnh có sẵn của
[CraftPix.net trên OpenGameArt](https://opengameart.org/content/cartoon-forest-2d-backgrounds)
theo OGA-BY 3.0; ảnh PNG gốc giữ nguyên, chỉ co giãn/cắt khung lúc hiển thị.
Không dùng ảnh tạo bởi AI. Nút **Nguồn ảnh** trong sảnh hiển thị attribution.
Icon từ [Kenney Crosshair Pack](https://kenney.nl/assets/crosshair-pack) (CC0),
font [Lilita One từ Google Fonts](https://github.com/google/fonts/tree/main/ofl/lilitaone)
(SIL OFL) được đóng kèm để chạy offline. Nguồn, checksum và giấy phép ở
`DartGame_Client/src/assets/CREDITS.md` và thư mục `assets/licenses` trong JAR.

Màn thi đấu có hai thẻ điểm lớn, đánh dấu người đang có lượt, ba kết quả phi tiêu
cho mỗi người, trạng thái xoay/ngắm/chờ xác nhận, phản hồi Bust và hướng dẫn luật.
Chat xuống dòng và tách khỏi vùng ngắm.

Điểm từng phi tiêu hiện ở hai nơi: popup to xuất hiện sau 280 ms khi phi tiêu
chạm bảng và số lớn **Lần ném vừa rồi** ở cột bên. Popup giữ khoảng 1,6 giây,
kết quả mới thay kết quả cũ; không nhận input và không chặn việc ngắm tiếp.
Bust có màu đỏ và ghi **không trừ điểm**; ném trượt ghi **0 / Trượt bảng**.
Các giá trị đều lấy từ kết quả server, không tính điểm lại ở client.

`ui/DartThrowEffects.java` vẽ phi tiêu bằng các shape JavaFX: bay trong 280 ms,
rung nhẹ và để lại phi tiêu tại tọa độ server xác nhận. Âm thanh `musics/dart.mp3`
được nạp bằng AudioClip và phát ở thời điểm chạm; nút Hiệu ứng bật/tắt âm thanh.
Phi tiêu của bạn có cánh xanh ngọc, đối thủ có cánh cam. Animation không chặn điều
khiển hoặc trì hoãn cập nhật điểm/lượt. Phi tiêu cuối vẫn bay hết khi server chuyển
lượt ngay, sau đó mờ đi; rời màn chơi hủy toàn bộ animation và âm thanh đang chờ.

Thay đổi chỉ nằm ở bố cục, màu sắc, chữ và cách trình bày dữ liệu server. Không
thay đổi luật, tính điểm, request, giao thức, transport hoặc dịch vụ server. Vùng
ngắm vẫn là 450×400, tâm (225, 200), cùng tốc độ và chuỗi thao tác X/Y trước đây.

Kiểm tra UI:

```sh
python3 scripts/desktop.py ui-test
# Tuỳ chọn xuất ảnh các màn bằng dữ liệu giả, không gửi lệnh tới server thật:
DARTS_UI_PREVIEW=build/ui-preview python3 scripts/desktop.py ui-test
```

Kiểm tra bao gồm nạp các FXML, đăng nhập thủ công, trạng thái chờ kết nối,
điều khiển xoay/ngắm, tọa độ gửi đi, một lệnh ném đang chờ xác nhận, khóa điều khiển
khi đối thủ có lượt, cửa sổ nhỏ, hướng dẫn không bị cắt và phản hồi Bust.
Kiểm tra animation chạy qua các FX pulse thực tế để xác nhận tọa độ đầu phi tiêu,
thời điểm gọi tiếng chạm, chuyển lượt trong khi bay và hủy khi rời màn chơi.

Cập nhật các bản để mở và gửi cho người chơi:

```sh
python3 scripts/desktop.py package client --type app-image
python3 scripts/desktop.py package client --type dmg
python3 scripts/package-player-downloads.py
```

Bộ cài ở `dist/Player-Downloads/Mac`, ZIP và thư mục Windows portable ở
`dist/Player-Downloads/Windows`. Lối tắt Desktop trỏ tới app tại
`dist/darwin-arm64/DartsBattle.app`. Chữ ký Mac và giới hạn kiểm thử Windows
giữ nguyên như trong hướng dẫn phát hành.
