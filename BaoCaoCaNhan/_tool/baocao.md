![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image1.png){width="7.091666666666667in"
height="10.2in"}

**HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG\
KHOA CÔNG NGHỆ THÔNG TIN 1**

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image2.png){width="1.41875in"
height="1.84375in"}

**BÁO CÁO MÔN HỌC\
LẬP TRÌNH MẠNG**

**Đề tài: Darts Battle - Game ném phi tiêu đối kháng online**

  ------------------- --------------------- ---------------
  **Lớp:**            D23CTPM01             

  **Nhóm học phần:**  CT01                  

  **Nhóm bài tập      Nhóm 04               
  lớn:**                                    

  **Thành viên:**     Nguyễn Nam Hải        B23DCCN277

                      Giáp Minh Hiếu        B23DCCN299

                      Văn Thị Mai Linh      B23DCCN489

                      Nguyễn Văn Minh Lực   B23DCCN516
  ------------------- --------------------- ---------------

**Hà Nội - 2026**

#### MỤC LỤC

*Nhấn chuột phải → Update Field (hoặc F9) để cập nhật mục lục.*

DANH MỤC TỪ VIẾT TẮT

  -----------------------------------------------------------------------
    **Từ viết   **Tiếng Anh**               **Ý nghĩa**
      tắt**                                 
  ------------- --------------------------- -----------------------------
       API      Application Programming     Giao diện lập trình ứng dụng
                Interface                   

      CSDL      --                          Cơ sở dữ liệu

       DTO      Data Transfer Object        Đối tượng truyền dữ liệu giữa
                                            Client và Server

       ER       Entity -- Relationship      Mô hình thực thể -- liên kết

      FXML      JavaFX Markup Language      Ngôn ngữ mô tả giao diện của
                                            JavaFX

       ICE      Interactive Connectivity    Cơ chế thiết lập kết nối của
                Establishment               WebRTC

      JDBC      Java Database Connectivity  API kết nối Java với CSDL

    JDK / JRE   Java Development Kit /      Bộ công cụ phát triển / môi
                Runtime Environment         trường chạy Java

       MVC      Model -- View -- Controller Mô hình kiến trúc phân tách
                                            dữ liệu, giao diện, điều
                                            khiển

       NAT      Network Address Translation Biên dịch địa chỉ mạng

       P2P      Peer-to-Peer                Kết nối ngang hàng trực tiếp
                                            giữa hai máy

     PBKDF2     Password-Based Key          Hàm băm mật khẩu có salt và
                Derivation Function 2       nhiều vòng lặp

       PvE      Player versus Environment   Người chơi đấu với máy (Bot)

       PvP      Player versus Player        Người chơi đấu với người chơi

       SDP      Session Description         Giao thức mô tả phiên đa
                Protocol                    phương tiện

   STUN / TURN  Session Traversal Utilities Dịch vụ hỗ trợ vượt NAT /
                for NAT / Traversal Using   chuyển tiếp media
                Relays around NAT           

       TCP      Transmission Control        Giao thức điều khiển truyền
                Protocol                    vận

       TLS      Transport Layer Security    Giao thức bảo mật tầng giao
                                            vận

       UI       User Interface              Giao diện người dùng

       WSS      WebSocket Secure            WebSocket chạy trên kết nối
                                            mã hóa
  -----------------------------------------------------------------------

DANH MỤC HÌNH VẼ

*Nhấn chuột phải → Update Field (hoặc F9) để cập nhật danh mục hình vẽ.*

DANH MỤC BẢNG BIỂU

*Nhấn chuột phải → Update Field (hoặc F9) để cập nhật danh mục bảng
biểu.*

PHÂN CÔNG CÔNG VIỆC VÀ TỈ LỆ ĐÓNG GÓP

Nhóm chia công việc theo nguyên tắc: mỗi lớp (class) chỉ có một người
phụ trách chính, các thành viên khác chỉ sử dụng các hàm công khai đã
thống nhất. Mọi thay đổi về bản tin (message) đều được cập nhật vào tài
liệu giao thức PROTOCOL.md; mỗi thành viên làm việc trên nhánh Git riêng
và tích hợp định kỳ. Công việc được chia theo bốn phân hệ, mỗi phân hệ
ứng với một nhóm chức năng F01--F10 trong danh sách yêu cầu chức năng ở
Chương 1 và gồm cả phần Client, Server và kiểm thử của phân hệ đó. Khối
lượng công việc của bốn thành viên được cân tương đương nhau.

1.  Phân công công việc và tỉ lệ đóng góp của các thành viên

+--------------+----------------+-------------------------------+------+
| **Thành      | **Phân hệ và   | **Công việc chi tiết**        | **Tỉ |
| viên**       | chức năng**    |                               | lệ** |
+:============:+================+===============================+======+
| Văn Thị Mai  | **Kết nối      | \- Dựng cấu trúc ba project   | 25%  |
| Linh         | mạng, xác thực | Client, Server, Common và     |      |
|              | và khung ứng   | định dạng bản tin dùng chung. |      |
| B23DCCN489   | dụng**         |                               |      |
|              |                | \- Kết nối client -- server   |      |
|              | F01 Tài khoản, | qua TCP có mã hóa TLS, ghim   |      |
|              | F02 Kết nối    | chứng chỉ máy chủ; máy chủ    |      |
|              |                | phục vụ nhiều client đồng     |      |
|              |                | thời.                         |      |
|              |                |                               |      |
|              |                | \- Đăng ký, đăng nhập, đăng   |      |
|              |                | xuất; băm mật khẩu PBKDF2;    |      |
|              |                | chặn một tài khoản đăng nhập  |      |
|              |                | trên hai máy.                 |      |
|              |                |                               |      |
|              |                | \- Giữ kết nối bằng           |      |
|              |                | heartbeat, phát hiện rớt mạng |      |
|              |                | và tự kết nối lại.            |      |
|              |                |                               |      |
|              |                | \- Đưa máy chủ ra Internet    |      |
|              |                | qua WebSocket và Cloudflare   |      |
|              |                | Tunnel; cài máy chủ thành     |      |
|              |                | dịch vụ chạy nền.             |      |
|              |                |                               |      |
|              |                | \- Khung cửa sổ, điều hướng   |      |
|              |                | giữa các màn hình, giao diện  |      |
|              |                | chung; màn hình đăng nhập,    |      |
|              |                | đăng ký và trang chủ.         |      |
|              |                |                               |      |
|              |                | \- Thiết kế bảng users; kiểm  |      |
|              |                | thử kết nối TLS, đăng nhập và |      |
|              |                | điều hướng.                   |      |
+--------------+----------------+-------------------------------+------+
| Nguyễn Nam   | **Sảnh chờ,    | \- Danh sách người chơi       | 25%  |
| Hải          | lời mời, phòng | online và trạng thái (rảnh,   |      |
|              | đấu và tương   | đang được mời, đang trong     |      |
| B23DCCN277   | tác**          | trận) cập nhật theo thời gian |      |
|              |                | thực.                         |      |
|              | F03 Sảnh chờ,  |                               |      |
|              | F04 Mời đấu,   | \- Gửi lời mời, hộp thoại     |      |
|              | F07 Tương tác  | Chấp nhận/Từ chối, lời mời tự |      |
|              | trong trận     | hết hạn sau 30 giây; chặn mời |      |
|              |                | chính mình hoặc người đang    |      |
|              |                | bận.                          |      |
|              |                |                               |      |
|              |                | \- Tạo và quản lý phòng đấu   |      |
|              |                | PvP/PvE; chơi lại nhiều ván   |      |
|              |                | trong cùng một phiên.         |      |
|              |                |                               |      |
|              |                | \- Chat trong trận; gọi       |      |
|              |                | thoại/video WebRTC giữa hai   |      |
|              |                | người chơi, cấp khóa TURN tạm |      |
|              |                | thời.                         |      |
|              |                |                               |      |
|              |                | \- Xử lý người chơi rời trận  |      |
|              |                | hoặc mất kết nối, thông báo   |      |
|              |                | cho đối thủ.                  |      |
|              |                |                               |      |
|              |                | \- Hệ thống hộp thoại và      |      |
|              |                | thông báo trong ứng dụng; màn |      |
|              |                | hình sảnh chờ.                |      |
|              |                |                               |      |
|              |                | \- Kiểm thử phiên chơi nhiều  |      |
|              |                | ván, cuộc gọi và giao diện    |      |
|              |                | tổng thể.                     |      |
+--------------+----------------+-------------------------------+------+
| Nguyễn Văn   | **Luồng trận   | \- Màn hình trận đấu dùng     | 25%  |
| Minh Lực     | đấu, cơ chế    | chung cho PvP và PvE: bàn phi |      |
|              | ném và luật    | tiêu, điểm còn lại, lượt,     |      |
| B23DCCN516   | chơi**         | lịch sử các lượt ném.         |      |
|              |                |                               |      |
|              | F06 Thi đấu    | \- Bàn xoay đầu lượt do máy   |      |
|              |                | chủ chọn góc; ngắm bằng hai   |      |
|              |                | vạch X/Y rồi ném.             |      |
|              |                |                               |      |
|              |                | \- Máy chủ xác thực từng phi  |      |
|              |                | tiêu: đúng trận, đúng lượt,   |      |
|              |                | tối đa ba phi tiêu, chống gửi |      |
|              |                | lặp.                          |      |
|              |                |                               |      |
|              |                | \- Tính vùng trúng và điểm    |      |
|              |                | theo luật 301; xử lý BUST,    |      |
|              |                | đổi lượt, hết thời gian lượt, |      |
|              |                | xử thua khi rời trận, kết     |      |
|              |                | thúc trận.                    |      |
|              |                |                               |      |
|              |                | \- Đồng bộ trạng thái trận    |      |
|              |                | cho hai client; hiệu ứng xoay |      |
|              |                | bàn, phi tiêu bay, điểm số và |      |
|              |                | âm thanh ném.                 |      |
|              |                |                               |      |
|              |                | \- Kiểm thử luật chơi và      |      |
|              |                | luồng trận nhiều người chơi   |      |
|              |                | (359 kiểm tra).               |      |
+--------------+----------------+-------------------------------+------+
| Giáp Minh    | **Bot, kết     | \- Chế độ chơi với Bot, 4 mức | 25%  |
| Hiếu         | quả, xếp hạng, | độ khó; Bot tự xoay bàn và    |      |
|              | lịch sử và     | ném theo đúng luật như người  |      |
| B23DCCN299   | phát hành**    | chơi.                         |      |
|              |                |                               |      |
|              | F05 Chơi với   | \- Lưu kết quả từng trận và   |      |
|              | Bot, F08 Xếp   | từng phi tiêu vào CSDL; thiết |      |
|              | hạng, F09 Lịch | kế bảng matches, match_throws |      |
|              | sử, F10 Cập    | và nâng cấp CSDL.             |      |
|              | nhật           |                               |      |
|              |                | \- Bảng xếp hạng PvP; lịch sử |      |
|              |                | đấu PvP/PvE và chi tiết từng  |      |
|              |                | lượt của một trận.            |      |
|              |                |                               |      |
|              |                | \- Màn hình kết quả cuối      |      |
|              |                | trận, âm thanh thắng/thua và  |      |
|              |                | nhạc nền.                     |      |
|              |                |                               |      |
|              |                | \- Đóng gói bản Windows bằng  |      |
|              |                | GitHub Actions, phát hành và  |      |
|              |                | tự cập nhật có chữ ký số.     |      |
|              |                |                               |      |
|              |                | \- Kiểm thử Bot, lưu và truy  |      |
|              |                | vấn lịch sử, nâng cấp CSDL và |      |
|              |                | bộ cập nhật.                  |      |
+--------------+----------------+-------------------------------+------+

*[\[Ghi chú: kiểm tra lại tỉ lệ đóng góp thực tế trước khi nộp; bôi đậm
tên của cá nhân viết báo cáo.\]]{.mark}*

LỜI MỞ ĐẦU

Lập trình mạng là học phần giúp sinh viên nắm vững cách các tiến trình
trên những máy tính khác nhau trao đổi dữ liệu với nhau thông qua mạng
máy tính: từ mô hình client -- server, lập trình socket TCP/UDP, thiết
kế giao thức tầng ứng dụng, xử lý đa luồng cho tới bảo mật đường truyền.
Để vận dụng các kiến thức đó vào một sản phẩm cụ thể, nhóm 04 lựa chọn
đề tài **"Darts Battle -- Game ném phi tiêu đối kháng online"**.

Trò chơi theo lượt là bài toán điển hình của lập trình mạng: nhiều người
chơi ở các máy khác nhau phải thấy cùng một trạng thái trận đấu, mọi
thao tác phải được đồng bộ gần như tức thời, máy chủ phải phục vụ đồng
thời nhiều kết nối và phải xử lý được các tình huống như rớt mạng, gửi
lệnh trùng hay gian lận. Darts Battle được xây dựng bằng Java, giao diện
JavaFX, truyền thông qua socket TCP có mã hóa TLS và lưu trữ dữ liệu
trên MySQL; hệ thống đã được triển khai để người chơi có thể kết nối qua
Internet mà không cần nhập địa chỉ IP.

Báo cáo gồm ba chương:

- **Chương 1 -- Giới thiệu và phân tích yêu cầu:** giới thiệu ứng dụng,
  luật chơi, công nghệ sử dụng, các yêu cầu chức năng và phi chức năng,
  nội dung cá nhân đảm nhận.

- **Chương 2 -- Phân tích thiết kế hệ thống:** kiến trúc tổng quan, giao
  thức truyền thông, sơ đồ khối chức năng của Client và Server, biểu đồ
  Use case tổng quan và thiết kế chi tiết phần cá nhân.

- **Chương 3 -- Kết quả thực hiện:** kiến trúc triển khai, cách cài đặt,
  các chức năng đã hoàn thành và kết quả kiểm thử.

Nhóm xin chân thành cảm ơn giảng viên bộ môn đã hướng dẫn trong suốt quá
trình thực hiện. Do thời gian và kinh nghiệm còn hạn chế, báo cáo không
tránh khỏi thiếu sót; nhóm rất mong nhận được góp ý để hoàn thiện sản
phẩm.

1.  GIỚI THIỆU VÀ PHÂN TÍCH YÊU CẦU

2.  Giới thiệu ứng dụng

3.  Bối cảnh và mục tiêu

Darts Battle là trò chơi mô phỏng môn ném phi tiêu (darts) chạy trên máy
tính, cho phép hai người chơi ở hai máy khác nhau thi đấu trực tiếp với
nhau qua mạng (PvP) hoặc một người chơi đấu với Bot do máy chủ điều
khiển (PvE). Ngoài trận đấu, ứng dụng cung cấp các chức năng cộng đồng
như danh sách người đang online, mời đấu, trò chuyện, gọi thoại/video
trong trận, bảng xếp hạng và lịch sử thi đấu chi tiết đến từng phi tiêu.

Mục tiêu kỹ thuật của đề tài là xây dựng một hệ thống client -- server
hoàn chỉnh, trong đó **máy chủ giữ toàn quyền quyết định**
(authoritative server): client chỉ gửi "ý định" của người chơi (ví dụ:
ném vào tọa độ x, y), còn việc kiểm tra lượt, tính điểm, đổi lượt và xác
định thắng thua đều do server thực hiện rồi gửi kết quả đã xác thực về
cho mọi client liên quan. Cách tiếp cận này giúp chống gian lận và bảo
đảm hai người chơi luôn nhìn thấy cùng một trạng thái trận đấu.

4.  Luật chơi

Trò chơi áp dụng thể thức **301 straight-out** quen thuộc của môn phi
tiêu, kết hợp cơ chế bàn xoay để tăng tính thử thách:

- Mỗi người chơi bắt đầu với 301 điểm; hai bên luân phiên lượt, mỗi lượt
  ném tối đa 3 phi tiêu.

- Đầu mỗi lượt, server chọn ngẫu nhiên một góc xoay nguyên trong khoảng
  0--359°; bàn phi tiêu phóng to, xoay nhiều vòng trong khoảng 2,8 giây
  rồi dừng ở góc đó. Chỉ khi bàn dừng hẳn người chơi mới được ném.

- Người chơi ngắm bằng hai vạch chạy: nhấp chuột lần thứ nhất để chốt
  tọa độ X, lần thứ hai để chốt tọa độ Y; giao điểm của hai vạch là vị
  trí phi tiêu.

- Điểm của một phi tiêu: vùng Single bằng điểm gốc của ô (1--20), Double
  gấp đôi, Triple gấp ba, Outer bull 25 điểm, Bullseye 50 điểm, ngoài
  bàn 0 điểm.

- Điểm phi tiêu được trừ vào điểm còn lại. Nếu điểm lớn hơn điểm còn lại
  (BUST) thì phi tiêu đó không được tính nhưng vẫn mất một lượt ném.
  Người về đúng 0 điểm thắng ngay lập tức.

- Người rời trận, mất kết nối hoặc hết thời gian lượt (120 giây) bị xử
  thua.

5.  Công nghệ sử dụng

<!-- -->

2.  Công nghệ và công cụ sử dụng trong dự án

  -----------------------------------------------------------------------
  **Thành phần** **Công nghệ**            **Vai trò**
  -------------- ------------------------ -------------------------------
     Ngôn ngữ    Java 17                  Ngôn ngữ chính cho cả Client,
                                          Server và thư viện Common

    Giao diện    JavaFX (FXML, CSS)       Xây dựng giao diện desktop,
                                          hiệu ứng bàn xoay, phi tiêu

   Truyền thông  Socket TCP, Java Object  Kết nối lâu dài, trao đổi đối
                 Serialization            tượng Message giữa Client --
                                          Server

     Bảo mật     TLS 1.2/1.3, certificate Mã hóa đường truyền, xác thực
                 pinning, PBKDF2          máy chủ, băm mật khẩu

     Internet    WebSocket (WSS),         Đưa máy chủ sau NAT ra Internet
                 Cloudflare Tunnel        qua tên miền

  Đa phương tiện WebRTC, Cloudflare       Gọi thoại/video P2P giữa hai
                 TURN/STUN                người chơi

  Cơ sở dữ liệu  MySQL 8, JDBC            Lưu người dùng, trận đấu, từng
                                          phi tiêu

  Kiểm thử, đóng H2 (in-memory),          Kiểm thử tích hợp, đóng gói ứng
       gói       jpackage, GitHub Actions dụng Windows kèm JRE
  -----------------------------------------------------------------------

6.  Phân tích yêu cầu hệ thống

7.  Tác nhân của hệ thống

- **Khách:** người đã cài ứng dụng nhưng chưa đăng nhập. Khách có thể
  đăng ký tài khoản, đăng nhập và cập nhật ứng dụng.

- **Người chơi:** người dùng đã đăng nhập thành công; có toàn bộ quyền
  của Khách cùng các chức năng sảnh chờ, thi đấu và thống kê.

- **Bot:** đối thủ ảo chạy ngay trên server, tự sinh tọa độ ném theo mức
  độ khó; Bot không phải tác nhân bên ngoài mà là một thành phần của hệ
  thống.

8.  Yêu cầu chức năng

<!-- -->

3.  Danh sách yêu cầu chức năng

  -------------------------------------------------------------------------
   **Mã**  **Nhóm chức     **Mô tả**
           năng**          
  -------- --------------- ------------------------------------------------
    F01    Tài khoản       Đăng ký (tên 3--32 ký tự, mật khẩu 6--128 ký
                           tự), đăng nhập, đăng xuất; mỗi tài khoản chỉ có
                           một phiên tại một thời điểm.

    F02    Kết nối         Client tự kết nối tới máy chủ khi mở ứng dụng,
                           tự kết nối lại khi mất mạng; người chơi không
                           phải nhập IP/cổng.

    F03    Sảnh chờ        Hiển thị danh sách người chơi online kèm trạng
                           thái (rảnh, đang được mời, đang trong trận), cập
                           nhật theo thời gian thực.

    F04    Mời đấu PvP     Gửi lời mời, nhận thông báo, chấp nhận hoặc từ
                           chối; lời mời tự hết hạn sau 30 giây; không mời
                           chính mình hoặc người đang bận.

    F05    Chơi với Bot    Tạo trận PvE với 4 mức độ khó: Dễ, Trung bình,
                           Khó, Cực khó.

    F06    Thi đấu         Bàn xoay đầu lượt, ngắm X/Y và ném; server tính
                           điểm, đổi lượt, xác định thắng thua; hiển thị
                           điểm, lượt, lịch sử ném.

    F07    Tương tác trong Chat văn bản, gọi thoại/video giữa hai người
           trận            chơi, rời trận, chơi lại nhiều ván trong một
                           phiên.

    F08    Xếp hạng        Bảng xếp hạng PvP sắp xếp theo số trận thắng;
                           trang thông tin người chơi.

    F09    Lịch sử         Xem lịch sử trận PvP và PvE của bản thân, xem
                           chi tiết từng lượt và từng phi tiêu của một
                           trận.

    F10    Cập nhật        Kiểm tra và tải bản cập nhật, xác minh chữ ký
                           trước khi cài đặt.
  -------------------------------------------------------------------------

9.  Yêu cầu phi chức năng

- **Tính nhất quán:** server là nơi duy nhất thay đổi trạng thái trận;
  lệnh trùng (cùng requestId) không được xử lý hai lần; client bỏ qua sự
  kiện cũ dựa trên số phiên bản (version) của trạng thái.

- **Đồng thời:** phục vụ nhiều client và nhiều trận cùng lúc; các trận
  khác nhau chạy độc lập, không chặn lẫn nhau.

- **Tin cậy:** phát hiện mất kết nối bằng heartbeat (PING mỗi 5 giây,
  hết hạn sau 45 giây); trận đấu luôn kết thúc đúng một lần với lý do rõ
  ràng.

- **Bảo mật:** mã hóa TLS và ghim chứng chỉ máy chủ, mật khẩu băm PBKDF2
  có salt, truy vấn bằng PreparedStatement, chỉ cho phép giải tuần tự
  hóa các lớp trong danh sách trắng.

- **Hiệu năng và trải nghiệm:** giao diện không bị treo khi chờ mạng;
  ghi lịch sử vào CSDL bất đồng bộ để không làm chậm trận đấu.

- **Triển khai:** client đóng gói kèm môi trường chạy Java, người chơi
  chỉ cần giải nén và mở ứng dụng.

10. Nội dung cá nhân đảm nhận

*[\[Phần cá nhân -- mỗi thành viên tự viết: giới thiệu phần việc mình
phụ trách trong tổng thể hệ thống, các chức năng, lớp và bản tin liên
quan, vai trò của phần việc đó đối với toàn bộ ứng dụng.\]]{.mark}*

11. PHÂN TÍCH THIẾT KẾ HỆ THỐNG

12. Kiến trúc tổng quan

Hệ thống được thiết kế theo mô hình **client -- server**, gồm ba project
Java tách biệt (Hình bên dưới):

- **DartGame_Client:** ứng dụng desktop JavaFX theo mô hình MVC. View là
  các tệp FXML/CSS; Controller xử lý thao tác người dùng; SocketHandler
  đảm nhiệm toàn bộ việc gửi/nhận bản tin. Client không tự tính điểm,
  chỉ hiển thị kết quả server gửi về.

- **DartGame_Server:** tiến trình máy chủ giữ toàn bộ trạng thái: người
  online, lời mời, phòng chơi, trạng thái trận; thực thi luật chơi, điều
  khiển Bot và ghi CSDL.

- **DartGame_Common:** thư viện dùng chung chứa định nghĩa bản tin
  (Message, MessageType, các DTO), bộ lọc giải tuần tự hóa WireFilter,
  lớp bảo mật đường truyền TransportSecurity và thông số hình học của
  bàn phi tiêu. Client và server phải được biên dịch cùng một phiên bản
  Common.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image3.png){width="6.4in"
height="3.95625in"}

1.  Kiến trúc tổng quan của hệ thống Darts Battle

Mỗi client duy trì **một kết nối TCP lâu dài** với server trong suốt
phiên chơi. Nhờ kết nối luôn mở, server có thể chủ động đẩy dữ liệu
xuống client bất cứ lúc nào (lời mời mới, đối thủ vừa ném, danh sách
online thay đổi) thay vì chờ client hỏi như mô hình HTTP. Phía server áp
dụng mô hình **thread-per-client**: mỗi kết nối được một luồng đọc riêng
và một luồng ghi riêng lấy dữ liệu từ hàng đợi; nhờ đó nhiều luồng (đối
thủ, Bot, bộ hẹn giờ) có thể cùng gửi cho một client mà không làm lẫn
byte trên socket, và một client mạng chậm không làm chậm cả server. Mọi
thay đổi trạng thái của một trận được thực hiện trong khối đồng bộ theo
phòng (synchronized(room)), các phòng khác nhau vẫn chạy song song.

Kênh truyền được phân thành nhiều tầng. Ứng dụng chỉ làm việc với đối
tượng Message; Java Serialization chuyển đối tượng thành chuỗi byte; TLS
mã hóa chuỗi byte đó; khi chơi qua Internet, luồng TLS được bọc thêm
trong WebSocket để đi qua Cloudflare Tunnel. Vì lớp WebSocketSocket giả
lập một Socket thông thường nên các tầng phía trên không phải thay đổi.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image4.png){width="5.6in"
height="2.6319444444444446in"}

2.  Các tầng giao thức trên kênh truyền Client -- Server

<!-- -->

13. Giao thức truyền thông

Đơn vị truyền là đối tượng **Message { type, requestId, payload }**,
trong đó type thuộc kiểu liệt kê MessageType (khoảng 47 loại), payload
là một DTO tương ứng. Bản tin chia thành hai nhóm: yêu cầu do client gửi
lên và phản hồi/sự kiện do server gửi xuống. Bảng dưới liệt kê các bản
tin chính.

4.  Các bản tin chính của giao thức

  -----------------------------------------------------------------------
            **Bản tin**            **Chiều**    **Ý nghĩa**
  -------------------------------- ------------ -------------------------
         REGISTER_REQUEST /        C → S / S →  Đăng ký tài khoản và kết
          REGISTER_RESULT          C            quả

    LOGIN_REQUEST / LOGIN_RESULT   C → S / S →  Đăng nhập; trả về userId,
                                   C            username

   LOGOUT_REQUEST / LOGOUT_RESULT  C → S / S →  Đăng xuất
                                   C            

            ONLINE_LIST            S → C        Danh sách người online và
                                                trạng thái

   INVITE_REQUEST, INVITE_ACCEPT,  C → S        Gửi, chấp nhận, từ chối
           INVITE_REJECT                        lời mời

  INVITE_RECEIVED / INVITE_RESULT  S → C        Thông báo lời mời đến và
                                                kết quả lời mời

          CREATE_BOT_MATCH         C → S        Tạo trận với Bot theo độ
                                                khó

   MATCH_STARTED, TURN_PREPARING,  S → C        Bắt đầu trận, góc xoay
   ROTATION_APPLIED, TURN_STARTED               đầu lượt, cho phép ném

          THROW_COORDINATE         C → S        Tọa độ ném (matchId,
                                                turnId, dartIndex,
                                                requestId, x, y)

           THROW_RESOLVED          S → C        Kết quả phi tiêu đã xác
                                                thực, gửi cho cả hai bên

           MATCH_FINISHED          S → C        Kết thúc trận, người
                                                thắng và lý do

            CHAT_MESSAGE           Hai chiều    Tin nhắn trong trận

            LEAVE_MATCH            C → S        Rời trận (bị xử thua)

  SESSION_READY / SESSION_LEAVE /  Hai chiều    Chơi lại nhiều ván trong
           SESSION_STATUS                       một phiên

     GET_RANKING, GET_HISTORY,     C → S        Yêu cầu xếp hạng, lịch
        MATCH_DETAIL_REQUEST                    sử, chi tiết trận

   MEDIA_JOIN, MEDIA_DESCRIPTION,  Hai chiều    Báo hiệu (signaling) cho
         MEDIA_CANDIDATE...                     cuộc gọi WebRTC

            PING / PONG            C → S / S →  Heartbeat giữ kết nối,
                                   C            phát hiện rớt mạng

               ERROR               S → C        Báo lỗi: sai lượt, dữ
                                                liệu không hợp lệ...
  -----------------------------------------------------------------------

Để bảo đảm tính nhất quán, mỗi lần ném mang một requestId duy nhất: nếu
client gửi lặp, server trả lại kết quả cũ mà không trừ điểm lần hai. Mỗi
ảnh chụp trạng thái trận (MatchSnapshot) có số version tăng dần; client
bỏ qua các sự kiện có version cũ, trùng hoặc thuộc trận khác. Các trường
turnId và dartIndex giúp server từ chối lệnh ném sai lượt hoặc đến muộn.

14. Sơ đồ khối chức năng của Client

Client được tổ chức thành năm khối chức năng. Khối **Kết nối & cập
nhật** chạy nền: tự kết nối tới máy chủ bằng TLS (qua WSS khi chơi
Internet), gửi PING định kỳ và tự kết nối lại. Các khối còn lại tương
ứng với các màn hình: tài khoản, sảnh chờ, trận đấu và thống kê. Mọi dữ
liệu nhận từ mạng được chuyển về luồng giao diện JavaFX bằng
Platform.runLater trước khi cập nhật màn hình.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image5.png){width="6.4in"
height="3.2930555555555556in"}

3.  Sơ đồ khối chức năng của Client

<!-- -->

15. Sơ đồ khối chức năng của Server

Server gồm sáu khối chức năng. Khối **Mạng & kết nối** nhận kết nối và
định tuyến bản tin tới các khối nghiệp vụ theo MessageType. Khối **Xác
thực & phiên** quản lý tài khoản và danh sách online. Khối **Sảnh &
phòng** quản lý lời mời và các phòng chơi trong bộ nhớ. Khối **Xử lý
trận đấu** là trung tâm, thực thi luật chơi và điều khiển Bot. Khối **Dữ
liệu & thống kê** ghi và truy vấn MySQL. Khối **Đa phương tiện** chỉ
chuyển tiếp tín hiệu khởi tạo cuộc gọi; âm thanh/hình ảnh đi trực tiếp
giữa hai client. Ngoài ra, một luồng bảo trì chạy mỗi giây để hủy lời
mời quá hạn và xử thua người chơi hết thời gian lượt.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image6.png){width="6.4in"
height="2.8381944444444445in"}

4.  Sơ đồ khối chức năng của Server

<!-- -->

16. Biểu đồ Use case tổng quan

Hệ thống có hai tác nhân. **Khách** chỉ có thể đăng ký, đăng nhập và cập
nhật ứng dụng. **Người chơi** kế thừa toàn bộ chức năng của Khách và sử
dụng các chức năng sảnh chờ, thi đấu, tương tác trong trận và thống kê.
Các use case của Người chơi đều yêu cầu đã đăng nhập thành công.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image7.png){width="6.2in"
height="4.528472222222222in"}

5.  Biểu đồ Use case tổng quan của hệ thống

<!-- -->

17. Phân tích thiết kế chi tiết chức năng cá nhân

*[\[Phần cá nhân -- mỗi thành viên tự viết cho chức năng mình phụ
trách.\]]{.mark}*

18. Use case chi tiết

*[\[Biểu đồ use case chi tiết và kịch bản (tác nhân, tiền điều kiện,
luồng chính, luồng phụ, hậu điều kiện).\]]{.mark}*

19. Biểu đồ lớp

*[\[Biểu đồ lớp của các lớp phía Client và Server liên quan đến chức
năng cá nhân.\]]{.mark}*

20. Biểu đồ tuần tự

*[\[Biểu đồ tuần tự trao đổi bản tin giữa Client -- Server -- CSDL cho
các chức năng chính.\]]{.mark}*

21. Sơ đồ thực thể quan hệ (ER)

*[\[Sơ đồ ER các bảng liên quan (users, matches, match_throws) và giải
thích các thuộc tính.\]]{.mark}*

22. KẾT QUẢ THỰC HIỆN

23. Kiến trúc ứng dụng triển khai

Hệ thống được triển khai thực tế với một máy chủ macOS và các máy người
chơi chạy Windows (Hình bên dưới). Máy chủ nằm sau NAT của mạng gia đình
nên không có địa chỉ IP công khai; thay vì mở cổng router, nhóm sử dụng
**Cloudflare Tunnel**: tiến trình cloudflared trên máy chủ chủ động tạo
đường hầm đi ra Cloudflare, và tên miền game-backsite.handq2212.site
được định tuyến vào đường hầm đó.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image8.png){width="6.4in"
height="3.561111111111111in"}

6.  Kiến trúc triển khai của ứng dụng: Server, Client và người chơi

Luồng kết nối của một người chơi như sau: ứng dụng client mở kết nối
wss://game-backsite.handq2212.site/play tới Cloudflare; Cloudflare
chuyển dữ liệu qua đường hầm về WebSocketBridge (127.0.0.1:8444) trên
máy chủ; bridge chuyển luồng byte sang Game Server TLS (127.0.0.1:8443).
Bên trong WebSocket vẫn là một phiên TLS đầu-cuối giữa client và game
server, client ghim chứng chỉ công khai của máy chủ nên Cloudflare chỉ
chuyển tiếp dữ liệu đã mã hóa. Game server truy cập MySQL qua localhost;
cổng CSDL không mở ra ngoài. Với cuộc gọi thoại/video, media đi P2P bằng
WebRTC, khi hai mạng không kết nối trực tiếp được thì đi qua máy chủ
TURN của Cloudflare. Bản cập nhật client được phát hành trên GitHub
Releases kèm tệp kê khai có chữ ký số.

24. Cài đặt và triển khai ứng dụng

25. Yêu cầu môi trường

<!-- -->

5.  Yêu cầu môi trường cài đặt

  -----------------------------------------------------------------------
      **Vai trò**     **Yêu cầu**
  ------------------- ---------------------------------------------------
   Máy phát triển /   JDK 17 trở lên, Python 3, Git; JavaFX được script
         build        build tự tải theo hệ điều hành

        Máy chủ       JDK 17+, MySQL 8.x, cloudflared (khi phục vụ qua
                      Internet), chứng chỉ và khóa TLS riêng

    Máy người chơi    Windows 10/11 x64; không cần cài Java, MySQL hay
                      VPN vì gói ứng dụng đã kèm JRE
  -----------------------------------------------------------------------

26. Cài đặt và khởi động máy chủ

**Bước 1 -- Kiểm tra môi trường.**

Mở Terminal trên máy chủ và kiểm tra phiên bản Java (yêu cầu từ 17 trở
lên) và Python 3.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image9.png){width="5.6in"
height="1.2430555555555556in"}

7.  Kiểm tra phiên bản Java và Python trên máy chủ

**Bước 2 -- Tải mã nguồn.**

Sao chép kho mã nguồn từ GitHub và chuyển sang nhánh phát triển. Thư mục
dự án gồm ba project DartGame_Common, DartGame_Client, DartGame_Server
cùng các script build trong thư mục scripts.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image10.png){width="5.6in"
height="2.967361111111111in"}

8.  Tải mã nguồn dự án từ GitHub

**Bước 3 -- Chuẩn bị cơ sở dữ liệu.**

Đăng nhập MySQL bằng tài khoản quản trị, tạo CSDL và tài khoản riêng cho
game. Các bảng users, matches, match_throws sẽ được server tự tạo ở lần
chạy đầu tiên.

> CREATE DATABASE IF NOT EXISTS dart_socket_game CHARACTER SET utf8mb4;
>
> CREATE USER IF NOT EXISTS \'darts\'@\'localhost\' IDENTIFIED BY
> \'\<mật_khẩu\>\';
>
> GRANT ALL PRIVILEGES ON dart_socket_game.\* TO
> \'darts\'@\'localhost\';

**Bước 4 -- Biên dịch.**

Chạy script build: script tự tải các thư viện còn thiếu (Java-WebSocket,
SLF4J, JavaFX, WebRTC) và tạo hai tệp darts-server.jar,
darts-client.jar.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image11.png){width="5.6in"
height="1.4895833333333333in"}

9.  Biên dịch Server và Client bằng script desktop.py

**Bước 5 -- Cấu hình và khởi động server.**

Thông tin kết nối CSDL, cổng và kho khóa TLS được truyền qua biến môi
trường (DARTS_DB_URL, DARTS_DB_USER, DARTS_DB_PASSWORD, DARTS_PORT,
DARTS_TLS_KEYSTORE, DARTS_TLS_PASSWORD), không ghi trong mã nguồn. Khi
khởi động, server kiểm tra/tạo bảng rồi báo sẵn sàng nhận kết nối TLS.
Trên máy chủ thật có thể chạy bằng lệnh python3 scripts/desktop.py run
server.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image12.png){width="5.6in"
height="1.1194444444444445in"}

10. Khởi động Game Server với kết nối TLS

**Bước 6 -- Chạy kiểm thử tự động.**

Trước khi đưa vào sử dụng, chạy bộ kiểm thử tích hợp để xác nhận luật
chơi, giao thức và CSDL hoạt động đúng.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image13.png){width="5.6in"
height="2.475in"}

11. Kết quả chạy bộ kiểm thử tự động

**Bước 7 -- Đưa máy chủ ra Internet.**

- Cài server thành dịch vụ chạy nền (launchd) bằng python3
  scripts/install-server-service.py để tự khởi động cùng hệ thống và tự
  chạy lại khi lỗi.

- Thiết lập Cloudflare Tunnel cho tên miền bằng python3
  scripts/connect-cloudflare.py; kiểm tra đầu-cuối bằng python3
  scripts/desktop.py probe (kết quả mong đợi: TLS được ghim và nhận được
  PONG).

- Tùy chọn: cấu hình khóa TURN của Cloudflare trong
  .local/turn.properties để hỗ trợ cuộc gọi qua các mạng khó kết nối.

27. Đóng gói và cài đặt Client

**Bước 1 -- Đóng gói tự động trên GitHub Actions.**

Mỗi lần đẩy mã lên nhánh phát triển, workflow Desktop packages chạy kiểm
thử và đóng gói client Windows x64 bằng jpackage (kèm JRE). Kết quả là
tệp DartsBattle-windows.zip.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image14.png){width="5.8in"
height="3.6902777777777778in"}

12. Các lần chạy workflow đóng gói trên GitHub Actions

**Bước 2 -- Tải bản phát hành.**

Bản phát hành được đăng trên GitHub Releases gồm gói
DartsBattle-windows.zip và tệp kê khai darts-update.txt có chữ ký số
(dùng cho chức năng tự cập nhật). Người chơi tải tệp ZIP về máy.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image15.png){width="5.8in"
height="3.3743055555555554in"}

13. Trang phát hành phiên bản 2.5.3 trên GitHub Releases

**Bước 3 -- Giải nén và mở ứng dụng.**

Giải nén toàn bộ tệp ZIP (gồm DartsBattle.exe, thư mục app và runtime),
sau đó mở DartsBattle.exe. Ứng dụng tự kết nối tới máy chủ đã cấu hình
sẵn; khi kết nối thành công, màn hình đăng nhập hiển thị trạng thái "Đã
kết nối".

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image16.jpeg){width="6.2in"
height="3.7381944444444444in"}

14. Màn hình đầu tiên khi mở ứng dụng: tự kết nối tới máy chủ

**Bước 4 -- Đăng ký tài khoản.**

Lần đầu sử dụng, người chơi chọn "Chưa có tài khoản? Đăng ký", nhập tên
đăng nhập (3--32 ký tự chữ, số hoặc dấu gạch dưới) và mật khẩu (6--128
ký tự).

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image17.jpeg){width="6.2in"
height="3.7381944444444444in"}

15. Màn hình đăng ký tài khoản

**Bước 5 -- Đăng nhập.**

Sau khi đăng ký thành công, ứng dụng quay lại màn hình đăng nhập. Ứng
dụng chỉ ghi nhớ tên đăng nhập, không lưu mật khẩu. Các phiên bản sau
được cập nhật ngay trong ứng dụng (nút "Kiểm tra cập nhật"): client tải
gói mới, kiểm tra chữ ký Ed25519 của tệp kê khai và mã băm SHA-256 trước
khi cài.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image18.jpeg){width="6.2in"
height="3.7381944444444444in"}

16. Màn hình đăng nhập

<!-- -->

28. Các kết quả thực hiện được

29. Các chức năng đã hoàn thành

<!-- -->

6.  Kết quả thực hiện các chức năng

  -----------------------------------------------------------------------
             **Chức năng**            **Kết quả**
  ----------------------------------- -----------------------------------
     Đăng ký, đăng nhập, đăng xuất    Hoàn thành; mật khẩu băm PBKDF2,
                                      chặn đăng nhập đồng thời

    Tự kết nối, tự kết nối lại qua    Hoàn thành; TLS + WSS qua
               Internet               Cloudflare Tunnel

  Danh sách online, mời đấu, hết hạn  Hoàn thành; cập nhật trạng thái
                lời mời               theo thời gian thực

  Trận PvP: bàn xoay, ngắm X/Y, luật  Hoàn thành; server tính điểm, chống
                  301                 lệnh trùng và sai lượt

     Trận PvE với Bot 4 mức độ khó    Hoàn thành

   Chat, gọi thoại/video trong trận   Hoàn thành; WebRTC P2P, TURN khi
                                      cần

  Chơi lại nhiều ván trong một phiên  Hoàn thành

    Rời trận, mất kết nối, hết thời   Hoàn thành; xử thua và lưu lý do
               gian lượt              kết thúc

   Bảng xếp hạng, lịch sử, chi tiết   Hoàn thành; xếp hạng chỉ tính trận
                 trận                 PvP

     Đóng gói Windows, tự cập nhật    Hoàn thành; phiên bản client 2.5.3
  -----------------------------------------------------------------------

30. Giao diện các chức năng chính

Các hình dưới đây được chụp từ ứng dụng thật khi chạy với hai tài khoản
thử nghiệm NamHai và MinhHieu.

**Sảnh chính.** Sau khi đăng nhập, người chơi vào sảnh chính với hai chế
độ: chơi với bạn bè (PvP) và chơi với Bot (PvE), cùng lối vào bảng xếp
hạng và lịch sử đấu.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image19.jpeg){width="6.2in"
height="3.7381944444444444in"}

17. Sảnh chính của ứng dụng

**Danh sách online và lời mời.** Màn hình "Chơi cùng bạn bè" hiển thị
người đang online kèm trạng thái. Khi có người mời, một hộp thông báo
xuất hiện với đồng hồ đếm ngược 30 giây và hai lựa chọn "Vào trận" hoặc
"Để lần sau".

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image20.jpeg){width="6.2in"
height="3.7381944444444444in"}

18. Danh sách người chơi online và hộp thoại nhận lời mời

**Trận đấu PvP.** Hai bên thấy cùng một trạng thái: điểm còn lại, người
đang đến lượt, vị trí các phi tiêu đã ném. Cột phải là khung trò chuyện;
khung camera và các nút micro/camera dùng cho cuộc gọi WebRTC. Người
chơi nhấp chuột để chốt vạch X, rồi nhấp lần nữa để chốt Y và ném.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image21.jpeg){width="6.2in"
height="3.7381944444444444in"}

19. Màn hình trận đấu PvP với khung trò chuyện

**Lịch sử các lượt.** Trong trận, người chơi có thể mở bảng lịch sử để
xem điểm từng phi tiêu, tổng điểm bị trừ và điểm còn lại sau mỗi lượt
của cả hai bên.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image22.jpeg){width="6.2in"
height="3.7381944444444444in"}

20. Bảng lịch sử các lượt trong trận

**Kết thúc trận.** Khi một bên về đúng 0 điểm, bỏ cuộc hoặc mất kết nối,
server gửi MATCH_FINISHED; client hiển thị kết quả, lý do kết thúc và
điểm cuối của hai bên.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image23.jpeg){width="6.2in"
height="3.7381944444444444in"}

21. Màn hình kết quả khi đối thủ bỏ cuộc

**Chơi với Bot.** Người chơi chọn một trong bốn mức độ khó; Bot chạy
trên server và ném theo đúng luật như người thật.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image24.jpeg){width="6.2in"
height="3.7381944444444444in"}

22. Hộp thoại chọn độ khó của Bot

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image25.jpeg){width="6.2in"
height="3.7381944444444444in"}

23. Trận đấu với Bot mức Trung bình

**Bảng xếp hạng.** Xếp hạng người chơi theo số trận thắng PvP; người
chơi xem được thứ hạng của mình và mở lịch sử của từng người.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image26.jpeg){width="6.2in"
height="3.7381944444444444in"}

24. Bảng xếp hạng PvP

**Lịch sử đấu và chi tiết trận.** Lịch sử liệt kê các trận PvP và PvE đã
hoàn thành, mới nhất trước; màn hình chi tiết hiển thị chế độ, người
thắng, lý do kết thúc và điểm từng phi tiêu của mỗi lượt.

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image27.jpeg){width="6.2in"
height="3.7381944444444444in"}

25. Lịch sử đấu của người chơi

![](/private/tmp/claude-501/-Users-handq2212--claude-spares-spare-ilrGAc/1e54dcbe-3a24-4e1f-8531-45363149233d/scratchpad/media/media/image28.jpeg){width="6.2in"
height="3.7381944444444444in"}

26. Chi tiết một trận đấu

<!-- -->

31. Kết quả kiểm thử

Bộ kiểm thử tự động (Hình kết quả chạy bộ kiểm thử ở mục cài đặt) khởi
động một server thật, dùng CSDL H2 trong bộ nhớ (chế độ tương thích
MySQL) và các socket TCP loopback thật, nên kiểm tra được toàn bộ đường
đi của bản tin mà không đụng tới dữ liệu thật. Kết quả lần chạy gần nhất
(23/11/2026):

7.  Tổng hợp kết quả kiểm thử

  ------------------------------------------------------------------------
  **Nhóm kiểm thử**  **Nội dung**                             **Kết quả**
  ------------------ ---------------------------------------- ------------
   Ngắm bắn của Bot  Phân bố tọa độ ném theo 4 mức độ khó     Đạt

   Gameplay qua TCP  Đăng ký/đăng nhập, nhiều phòng đồng      359/359 đạt
                     thời, các tổ hợp ô điểm -- vành -- góc   
                     xoay, BUST, thắng, lệnh trùng/sai        
                     lượt/sai người, payload sai, chat, Bot,  
                     rời trận, mất kết nối, hết hạn lời       
                     mời/lượt                                 

  Phiên chơi và lịch Chơi lại nhiều ván, lưu và truy vấn lịch Đạt
          sử         sử, chi tiết trận                        

   Transport / TLS   Bắt tay TLS, ghim chứng chỉ, từ chối     17/17 đạt
                     chứng chỉ lạ, giới hạn frame WebSocket   

     Gameplay qua    Chạy lại toàn bộ kịch bản gameplay qua   359/359 đạt
   WebSocket + TLS   kênh WSS có TLS ghim chứng chỉ           

    Thử nghiệm thủ   Hai client đăng nhập, mời -- chấp nhận,  Hoạt động
         công        chơi PvP có chat, bỏ cuộc, chơi với Bot, đúng
                     xem xếp hạng/lịch sử (các hình ở mục     
                     trên)                                    
  ------------------------------------------------------------------------

32. Đánh giá và hướng phát triển

**Ưu điểm:** hệ thống hoạt động ổn định theo mô hình server toàn quyền,
chống được gian lận và lệnh trùng; kết nối được qua Internet mà người
chơi không cần cấu hình; đường truyền được mã hóa đầu-cuối; có bộ kiểm
thử tự động bao phủ các luồng nghiệp vụ chính.

**Hạn chế:** chưa khôi phục được trận đang chơi khi server khởi động lại
hoặc khi người chơi rớt mạng; mỗi phòng chỉ hỗ trợ hai người; client
macOS tạm dừng phát hành; lịch sử đang chờ ghi có thể mất nếu server bị
dừng đột ngột.

**Hướng phát triển:** hỗ trợ kết nối lại vào trận cũ, chế độ nhiều người
chơi trong một phòng, giải đấu; lưu hàng đợi lịch sử bền vững (outbox);
phát hành thêm phiên bản macOS đã ký và các thể thức chơi khác như 501,
Cricket.

KẾT LUẬN

Qua quá trình thực hiện đề tài Darts Battle, nhóm đã xây dựng thành công
một trò chơi đối kháng trực tuyến hoàn chỉnh theo mô hình client --
server, vận dụng trực tiếp các kiến thức của học phần Lập trình mạng:
lập trình socket TCP, thiết kế giao thức tầng ứng dụng, xử lý đa luồng
và đồng bộ, phát hiện mất kết nối bằng heartbeat, bảo mật đường truyền
bằng TLS và đưa dịch vụ ra Internet qua đường hầm. Sản phẩm đã được đóng
gói, triển khai và kiểm thử tự động với kết quả đạt yêu cầu. Bên cạnh
kiến thức chuyên môn, các thành viên cũng rèn luyện được kỹ năng làm
việc nhóm, phân chia công việc theo module và quản lý mã nguồn bằng Git.

TÀI LIỆU THAM KHẢO

1.  Oracle, "Java Platform, Standard Edition 17 API Specification --
    java.net, javax.net.ssl, java.io",
    https://docs.oracle.com/en/java/javase/17/docs/api/

2.  Oracle, "The jpackage Command (JDK 17)",
    https://docs.oracle.com/en/java/javase/17/docs/specs/man/jpackage.html

3.  OpenJFX, "Getting Started with JavaFX",
    https://openjfx.io/openjfx-docs/

4.  E. Harold, Java Network Programming, 4th ed., O'Reilly Media, 2013.

5.  J. F. Kurose, K. W. Ross, Computer Networking: A Top-Down Approach,
    8th ed., Pearson, 2021.

6.  E. Rescorla, "The Transport Layer Security (TLS) Protocol Version
    1.3", RFC 8446, IETF, 2018.

7.  I. Fette, A. Melnikov, "The WebSocket Protocol", RFC 6455, IETF,
    2011.

8.  K. Moriarty et al., "PKCS #5: Password-Based Cryptography
    Specification Version 2.1", RFC 8018, IETF, 2017.

9.  Cloudflare, "Cloudflare Tunnel documentation",
    https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/

10. W3C, "WebRTC: Real-Time Communication in Browsers",
    https://www.w3.org/TR/webrtc/

11. Oracle, "MySQL 8.4 Reference Manual",
    https://dev.mysql.com/doc/refman/8.4/en/
