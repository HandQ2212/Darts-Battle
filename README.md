## I. GIỚI THIỆU VỀ GAME (INTRODUCTION)

1. Dart game:

- Là dự án bài tập lớn thuộc bộ môn Lập trình mạng của HVCNBCVT (PTIT)
- Là 1 trò chơi mô phòng dựa trên game ném phi tiêu. Hệ thống hỗ trợ 2 chế độ chơi chính là PvC và PvE

2. Công nghệ sử dụng:
   1. Java FX
   2. JDBC
   3. MySQL
3. Các chức năng chính và minh họa:
   1. Đăng nhập đăng kí
      - ![alt text](image-10.png)
      - ![alt text](image-11.png)
   2. Chơi với máy (PvC)
      - ![alt text](image-12.png)
   3. Mời người chơi khác (PvP)
      - ![alt text](image-13.png)
   4. Xem Bảng xếp hạng
   5. Lịch sử đấu

## II. HƯỚNG DẪN SETUP MÔI TRƯỜNG (SETUP TURTORIAL):

1. Netbean:
   1. Thư viện:
      1. ![alt text](image.png)
      2. Có thể tải luôn ở file Drive: `https://drive.google.com/drive/folders/1Unyrvpwn3IzNGO_zoOBSkXxevbMlOlKw?usp=sharing`
      3. ZuluFX, JDK 17
         1. https://www.azul.com/downloads/?version=java-17-lts&os=windows&architecture=x86-64-bit&package=jdk-fx#zulu
         2. ![alt text](image-2.png)
      4. My SQL J
   2. Cách cài đặt:
      1. Vào Project, chọn Add Jar:
         1. ![alt text](image-3.png)
      2. Với ZULU thì phải thêm nó vào Platform:
         1. `http://youtube.com/watch?v=WQZpIZeHzLM&t=59s`
2. Database:
   1. Dùng Mysql (DBMS thì XAMPP hoặc Workbench đều được)
   2. Tạo DB dart_socket_game với bảng Users, MatchHistory với các cột sau:
      1. ![alt text](image-5.png)
      2. ![alt text](image-7.png)
      3. ![alt text](image-8.png)
      4. ![alt text](image-9.png)
   3. Đổi mkhau - tkhoanr db trong code (dbconnection.java)
      1. ![alt text](image-6.png)
3. Khởi chạy:
   1. Nếu muốn Test trên 1 máy:
      1. Bật Server trước => Bật 2 Client, mỗi Client đăng nhập 1 tài khoản
   2. Test trên nhiều máy:
      1. Cài đặt Radmin VPN, 2 máy vào chung 1 mạng.
      2. 1 máy bật Server, nhiều máy bật Client
