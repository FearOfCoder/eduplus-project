<div align="center">
  <img src="src/main/resources/static/images/logoEduPlus.jpg" alt="EduPlus Logo" width="150" height="150">
  <h1>🌟 EduPlus - Nền tảng Học trực tuyến Tiên tiến</h1>
  <p><i>Đột phá không gian học tập với công nghệ Trí tuệ Nhân tạo & Trải nghiệm người dùng đỉnh cao</i></p>

  <p>
    <img src="https://img.shields.io/badge/Spring_Boot-F2F4F9?style=for-the-badge&logo=spring-boot" alt="Spring Boot">
    <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white" alt="Java">
    <img src="https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
    <img src="https://img.shields.io/badge/Bootstrap-563D7C?style=for-the-badge&logo=bootstrap&logoColor=white" alt="Bootstrap">
  </p>
</div>

---

## 📖 Giới thiệu (About)

**EduPlus** là một nền tảng giáo dục trực tuyến (E-Learning) toàn diện, kết nối Giảng viên và Học viên thông qua các khóa học chất lượng cao. Dự án được xây dựng với kiến trúc nguyên khối (Monolithic) tối ưu, giao diện người dùng (UI/UX) mang phong cách hiện đại (Vibrant, Card-based), và đặc biệt tích hợp các tính năng AI hỗ trợ học tập và chấm điểm.

Hệ thống được thiết kế với tư duy của một sản phẩm thương mại thực thụ, bao gồm quy trình phê duyệt khắt khe, bảng điều khiển quản trị (Admin Dashboard) mạnh mẽ và hệ thống xử lý giao dịch an toàn.

---

## ✨ Tính năng Nổi bật (Key Features)

### 🎓 Dành cho Học viên (Student Workspace)
- **Khám phá thông minh:** Mega Menu và Smart Search tích hợp gợi ý từ khóa "Đang thịnh hành" chuẩn UX quốc tế.
- **Không gian học tập (Workspace):** Trình phát video bài giảng mượt mà, kết hợp khu vực làm bài tập và hỏi đáp trực tiếp.
- **Chấm điểm bằng AI:** Tự động phân tích, chấm điểm mã nguồn (code) hoặc câu trả lời tự luận, chỉ ra lỗi sai và gợi ý tối ưu.
- **Theo dõi tiến độ:** Thống kê quá trình học tập trực quan.

### 👨‍🏫 Dành cho Giảng viên (Teacher Portal)
- **Tạo & Quản lý Khóa học:** Trình soạn thảo trực quan giúp cấu trúc bài giảng, video và tài liệu đính kèm.
- **Quản lý Tài chính:** Theo dõi doanh thu từ các khóa học đã bán và gửi yêu cầu rút tiền về tài khoản ngân hàng.
- **Tương tác:** Quản lý danh sách học viên và phản hồi đánh giá.

### 👑 Quản trị viên (Admin Control Panel)
- **Modern Dashboard:** Giao diện quản trị dạng thẻ (Card-list Table) hiện đại, tích hợp công cụ lọc (Filter/Sort) bằng DataTables siêu tốc mà không cần tải lại trang.
- **Quyền sinh sát tối thượng:** - Duyệt/Từ chối, Tạm dừng hoạt động hoặc Xóa vĩnh viễn khóa học.
  - Xem hồ sơ chi tiết (Smart Popup), Cấm (Ban) hoặc Xóa tài khoản vi phạm.
- **Hệ thống Cài đặt Động:** Admin có quyền thay đổi Logo hệ thống, cấu hình phí nền tảng và các điều khoản trực tiếp từ giao diện.
- **Trung tâm trợ giúp (FAQ):** Quản lý các câu hỏi thường gặp theo cấu trúc danh mục thông minh (Notion-style).

---

## 🛠️ Công nghệ Sử dụng (Tech Stack)

### Backend
* **Ngôn ngữ:** Java 17+
* **Framework:** Spring Boot 3.x
* **Bảo mật:** Spring Security (Xác thực, Phân quyền Role-based, Chặn truy cập tài khoản Banned)
* **ORM & Database:** Spring Data JPA, Hibernate, MySQL

### Frontend
* **Template Engine:** Thymeleaf
* **Giao diện:** HTML5, CSS3, Bootstrap 5
* **Tương tác & Xử lý dữ liệu:** Vanilla JavaScript, jQuery, DataTables
* **Thành phần bổ trợ:** FontAwesome (Icons), UI-Avatars (Fallback Images)

---

## 📸 Giao diện Hệ thống (Screenshots)

<details>
  <summary><b>Nhấn để xem ảnh chụp màn hình Giao diện</b></summary>
  
  > **Lưu ý:** *Thay thế đường dẫn ảnh thực tế của bạn vào đây sau khi đã đẩy lên GitHub.*
  
  **1. Trang chủ & Mega Menu Khám phá**
  <img src="link_anh_trang_chu_cua_ban.png" alt="Trang chủ">

  **2. Bảng điều khiển Quản trị viên (Admin Dashboard)**
  <img src="link_anh_admin_cua_ban.png" alt="Admin Dashboard">

  **3. Tính năng Xem chi tiết Người dùng (Smart Popup)**
  <img src="link_anh_popup_cua_ban.png" alt="User Popup">
</details>

---

## ⚙️ Hướng dẫn Cài đặt (Installation)

### Yêu cầu môi trường
- Java Development Kit (JDK) 17 hoặc mới hơn
- Maven 3.6+
- MySQL Server 8.0+

### Các bước triển khai
1. **Clone repository về máy:**
   ```bash
   git clone [https://github.com/TenTaiKhoanCuaBan/eduplus.git](https://github.com/TenTaiKhoanCuaBan/eduplus.git)
   cd eduplus
   Cấu hình Cơ sở dữ liệu:

Tạo một database trong MySQL có tên là eduplus_db.

Mở file src/main/resources/application.properties và cập nhật thông tin đăng nhập:

Properties
spring.datasource.url=jdbc:mysql://localhost:3306/eduplus_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=mat_khau_cua_ban
Chạy ứng dụng:

Sử dụng IDE (IntelliJ, Eclipse, Cursor) để chạy class EduplusApplication.java.

Hoặc chạy bằng lệnh Maven Terminal:

Bash
mvn spring-boot:run
Trải nghiệm:

Truy cập vào địa chỉ: http://localhost:8080

Tài khoản Admin mặc định (Nên được cấu hình sẵn trong Database Seeder):

Email: admin@example.com

Mật khẩu: 123456 (Hoặc mật khẩu bạn đã cài)

<div align="center">
<b>Được phát triển với ❤️ và rất nhiều ☕</b>
</div>