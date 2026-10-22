# RMS Lite — Recruitment Management System (HSF302 · Group 3)

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
| Bảo mật | Spring Security — *bổ sung ở Sprint 3* |

## Chạy dự án lần đầu

1. Cài **JDK 21** và **SQL Server**, tạo database rỗng `RMS_DB`. Bảng do Hibernate tự tạo từ entity.
2. Copy (không rename) file cấu hình mẫu rồi điền mật khẩu SQL Server của máy bạn:
   ```bash
   cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
   ```
3. Chạy ứng dụng:
   ```bash
   ./mvnw spring-boot:run        # macOS / Linux / Git Bash
   mvnw.cmd spring-boot:run      # Windows CMD / PowerShell
   ```
4. Mở <http://localhost:8080/demo/hello> và <http://localhost:8080/demo/db-check> để kiểm tra cài đặt.
   Package `demo/` chỉ dùng tạm cho bước này và sẽ được xóa khi có trang chủ thật.

## Chạy test

```bash
./mvnw verify
```

Test khởi động toàn bộ ứng dụng nên cần SQL Server đang chạy và `application-local.properties` đã cấu hình, giống khi chạy app.

## Cấu trúc dự kiến

> Cấu trúc dưới đây là dự kiến, sẽ điều chỉnh khi bắt đầu code.

```
src/main/java/com/group3/rms
├── common/         Dùng chung: config, BaseEntity, xử lý lỗi, người dùng hiện tại, lưu file
├── user/           Tài khoản, vai trò
├── audit/          Nhật ký hoạt động
├── job/            Tin tuyển dụng
├── application/    Hồ sơ ứng tuyển
└── interview/      Phỏng vấn, đánh giá
```

Chia package theo tính năng; mỗi tính năng gồm `controller · service · repository · entity · dto · mapper`.

## Quy trình làm việc

### Nhánh

- `main` chỉ nhận thay đổi qua **Pull Request**, không push thẳng.
- Tên nhánh: `<loại>/<tên-thành-viên>/<mô-tả-ngắn>`, viết thường, không dấu, nối bằng `-`:

| Loại | Dùng khi | Ví dụ |
| :--- | :--- | :--- |
| `feature` | Thêm tính năng | `feature/dunglt/tao-tin-tuyen-dung` |
| `fix` | Sửa lỗi | `fix/viethq/chan-nop-trung` |
| `refactor` | Sửa cấu trúc code, không đổi hành vi | `refactor/dunglt/tach-job-service` |
| `docs` | Viết / sửa tài liệu | `docs/viethq/architecture` |
| `chore` | Cấu hình, build, thư viện | `chore/viethq/cap-nhat-pom` |

### Pull Request

- Mỗi phần việc một PR riêng: một tính năng, một lần sửa lỗi, một lần cập nhật tài liệu. Không gộp các việc không liên quan vào cùng một PR.
- Chạy `./mvnw verify` xanh trên máy rồi mới mở PR; cần **ít nhất 1 người review** trước khi merge.
- Cập nhật `main` vào nhánh của mình thường xuyên để xung đột nhỏ và sớm.

### Commit

Theo [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<phạm vi>): <mô tả>`. Phạm vi không bắt buộc; mô tả viết tiếng Anh hoặc tiếng Việt đều được.

| Type | Ý nghĩa |
| :--- | :--- |
| `feat` | Tính năng mới |
| `fix` | Sửa lỗi |
| `refactor` | Sửa cấu trúc code, không đổi hành vi |
| `docs` | Tài liệu |
| `style` | Định dạng, giao diện (CSS), không đổi logic |
| `test` | Thêm / sửa test |
| `chore` | Cấu hình, build, thư viện |

Ví dụ: `feat(job): tạo tin tuyển dụng` · `fix(application): prevent duplicate apply` · `docs: cập nhật README`
