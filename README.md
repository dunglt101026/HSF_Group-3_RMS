# RMS — Recruitment Management System (HSF302 · Group 3)

Hệ thống quản lý tuyển dụng nội bộ: HR đăng tin, nhận hồ sơ, xếp lịch phỏng vấn, thu đánh giá và chốt kết quả; ứng viên tự theo dõi hồ sơ của mình. Giao diện render phía server bằng Thymeleaf.

## Công nghệ

| Thành phần | Lựa chọn |
| :--- | :--- |
| Ngôn ngữ / Build | Java 21, Maven (dùng `mvnw`, hoặc `mvn` riêng) |
| Framework | Spring Boot 4 (Web MVC, Validation, Data JPA) |
| Giao diện | Thymeleaf + Thymeleaf Layout Dialect |
| CSDL | Microsoft SQL Server |
| Mapping | MapStruct (Entity ↔ DTO), DTO dạng Java `record` |
| Tiện ích | Lombok, Spring Boot DevTools |
| Bảo mật | Mật khẩu BCrypt; Spring Security bổ sung ở giai đoạn cuối |

## Thư mục

| Đường dẫn | Nội dung |
| :--- | :--- |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Kiến trúc và quy ước code — đọc trước khi code |
| [`docs/database.md`](docs/database.md) | Đặc tả 10 bảng, cách reset DB, tài khoản mẫu |
| `database/seed.sql` | Dữ liệu mẫu |
| `src/main/java/com/group3/rms/` | Code, chia package theo chức năng: `common`, `user`, `audit`, `job`, `application`, `interview` |
| `src/main/resources/` | `application.properties`, `templates/`, `static/` |

## Chạy dự án lần đầu

1. Cài **JDK 21** và **SQL Server**, tạo database rỗng `RMS_DB`.
2. Copy (không rename) file cấu hình mẫu rồi điền mật khẩu SQL Server của máy bạn:
   ```bash
   cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
   ```
3. Chạy ứng dụng (bảng tự sinh), rồi dừng app:
   ```bash
   ./mvnw spring-boot:run        # macOS / Linux / Git Bash
   mvnw.cmd spring-boot:run      # Windows CMD / PowerShell
   ```
4. Chạy `database/seed.sql` trên `RMS_DB` để có dữ liệu mẫu (xem [docs/database.md](docs/database.md) mục 5).
5. Chạy lại app, mở <http://localhost:8080/jobs>; thấy danh sách việc làm là đã kết nối được database.

Chạy test: `./mvnw verify` (cần SQL Server đang chạy).

## Quy tắc Git

- `main` khóa, chỉ nhận qua Pull Request. Tạo nhánh từ `main` mới nhất: `<loại>/<tên-mình>/<việc>`, ví dụ `feature/dunglt/uc12-tao-tin`, `fix/viethq/chan-nop-trung`.
- Commit: `loại(phạm vi): mô tả`, ví dụ `feat(job): tao tin tuyen dung`. Loại: `feat`, `fix`, `refactor`, `docs`, `style`, `test`, `chore`.
- 1 PR = 1 Use Case. Chạy `mvnw verify` xanh rồi mới mở PR; cần 1 người approve rồi tự merge (*Squash and merge*).
- PR có sửa entity: tiêu đề thêm `[DB]`, sửa luôn `seed.sql` và `docs/database.md`.
- Cập nhật `main` vào nhánh mình thường xuyên. Lỡ merge nhầm: bấm **Revert** trên PR, không sửa thẳng `main`.
