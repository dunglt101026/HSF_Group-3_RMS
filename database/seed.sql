/* =====================================================================
   RMS — DỮ LIỆU MẪU (SQL Server)

   Cách chạy: tạo DB rỗng RMS_DB → chạy app 1 lần để Hibernate sinh bảng → dừng app → chạy file này.
     - SSMS / Azure Data Studio: mở file, chọn database RMS_DB, Execute.
     - sqlcmd:  sqlcmd -S localhost -E -C -d RMS_DB -f 65001 -i database/seed.sql
   Chạy lại bao nhiêu lần cũng được: script xóa sạch dữ liệu cũ rồi nạp lại.
   Ngày giờ tính theo lúc chạy script (tin ACTIVE luôn còn hạn, lịch phỏng vấn luôn có quá khứ / tương lai).

   TÀI KHOẢN MẪU — mật khẩu chung cho môi trường dev: Test@123
     admin                       ADMIN
     hr1, hr2                    HR
     interviewer1, interviewer2  INTERVIEWER
     candidate1 … candidate7     CANDIDATE (tên, email, số điện thoại khớp CV mẫu trong static/sample/cv)

   CV: mỗi hồ sơ trỏ tới một file PDF mẫu ('seed/<tên file>'), nên "Tải CV" chạy được ngay,
   không cần chép file vào uploads/cv.

   Kịch bản demo (chạy lại script để đưa dữ liệu về trạng thái này):
     - hr1: hồ sơ #1 Khoa (APPLIED) → bấm Duyệt rồi Lên lịch; hồ sơ #2 Huy (APPLIED) → thử Loại.
     - hr2: hồ sơ #7 Hương (INTERVIEW, mọi vòng đã đánh giá) → thử Tuyển / Loại;
            hồ sơ #6 Ngọc Anh còn vòng 2 sắp tới → Tuyển bị chặn (cần đánh giá hết các vòng).
     - interviewer2: hồ sơ #3 Hà có buổi đã qua giờ CHƯA đánh giá (UC-25).
     - interviewer1: buổi sắp tới của hồ sơ #6 vòng 2 (UC-24); đã đánh giá hồ sơ #5, #8.
     - hồ sơ #5 Bảo (HIRED, 2 vòng), #8 Huy (REJECTED sau vòng 1), #4 Long (WITHDRAWN).
     - Lưu ý: lịch phỏng vấn mới tạo luôn ở tương lai nên chưa đánh giá được ngay; muốn thử UC-25
       dùng buổi #1 ở trên.

   Sửa entity → sửa file này trong cùng PR (tiêu đề PR có [DB]).
   ===================================================================== */

SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @now   DATETIME2 = SYSDATETIME();
DECLARE @today DATETIME2 = CAST(CAST(@now AS DATE) AS DATETIME2);   -- 00:00 hôm nay
DECLARE @pwd   NVARCHAR(255) = N'$2a$10$CR5LwsVyBlUmK87YhIThOOTV/BBgpvzQmlXtGZH9iHwozmfkXPLy2';  -- Test@123

/* ---------- 1. Xóa dữ liệu cũ (bảng con trước, bảng cha sau) ---------- */
DELETE FROM audit_logs;
DELETE FROM interviews;
DELETE FROM applications;
DELETE FROM job_skills;
DELETE FROM jobs;
DELETE FROM user_roles;
DELETE FROM users;
DELETE FROM skills;
DELETE FROM departments;
DELETE FROM roles;

/* ---------- 2. Vai trò ---------- */
SET IDENTITY_INSERT roles ON;
INSERT INTO roles (id, code, name) VALUES
(1, 'ADMIN',       N'Quản trị viên'),
(2, 'HR',          N'Nhân viên tuyển dụng'),
(3, 'INTERVIEWER', N'Người phỏng vấn'),
(4, 'CANDIDATE',   N'Ứng viên');
SET IDENTITY_INSERT roles OFF;

/* ---------- 3. Tài khoản ---------- */
SET IDENTITY_INSERT users ON;
INSERT INTO users (id, username, email, password_hash, full_name, phone, status, created_at, updated_at, created_by, updated_by) VALUES
(1,  'admin',        'admin@rms.local',        @pwd, N'Quản Trị Hệ Thống', NULL,         'ACTIVE', DATEADD(DAY, -60, @now), NULL, 'system', NULL),
(2,  'hr1',          'hr1@rms.local',          @pwd, N'Đinh Thị Lan',      '0901000001', 'ACTIVE', DATEADD(DAY, -50, @now), NULL, 'admin',  NULL),
(3,  'hr2',          'hr2@rms.local',          @pwd, N'Lê Minh Quân',      '0901000002', 'ACTIVE', DATEADD(DAY, -50, @now), NULL, 'admin',  NULL),
(4,  'interviewer1', 'interviewer1@rms.local', @pwd, N'Phạm Đức Anh',      '0901000003', 'ACTIVE', DATEADD(DAY, -45, @now), NULL, 'admin',  NULL),
(5,  'interviewer2', 'interviewer2@rms.local', @pwd, N'Võ Thị Mai',        '0901000004', 'ACTIVE', DATEADD(DAY, -45, @now), NULL, 'admin',  NULL),
-- Ứng viên: họ tên, email, số điện thoại lấy đúng theo CV mẫu trong static/sample/cv
(6,  'candidate1',   'candidate1@example.com', @pwd, N'Nguyễn Minh Khoa',  '0900000101', 'ACTIVE', DATEADD(DAY, -40, @now), NULL, 'system', NULL),
(7,  'candidate2',   'candidate2@example.com', @pwd, N'Trần Thu Hà',       '0900000102', 'ACTIVE', DATEADD(DAY, -38, @now), NULL, 'system', NULL),
(8,  'candidate3',   'candidate3@example.com', @pwd, N'Lê Quốc Bảo',       '0900000103', 'ACTIVE', DATEADD(DAY, -37, @now), NULL, 'system', NULL),
(9,  'candidate4',   'candidate4@example.com', @pwd, N'Phạm Ngọc Anh',     '0900000104', 'ACTIVE', DATEADD(DAY, -36, @now), NULL, 'system', NULL),
(10, 'candidate5',   'candidate5@example.com', @pwd, N'Đỗ Thanh Hương',    '0900000105', 'ACTIVE', DATEADD(DAY, -35, @now), NULL, 'system', NULL),
(11, 'candidate6',   'candidate6@example.com', @pwd, N'Vũ Hoàng Long',     '0900000106', 'ACTIVE', DATEADD(DAY, -34, @now), NULL, 'system', NULL),
(12, 'candidate7',   'candidate7@example.com', @pwd, N'Hoàng Gia Huy',     '0900000107', 'ACTIVE', DATEADD(DAY, -33, @now), NULL, 'system', NULL);
SET IDENTITY_INSERT users OFF;

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),
(2, 2), (3, 2),
(4, 3), (5, 3),
(6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (11, 4), (12, 4);

/* ---------- 4. Phòng ban, kỹ năng ---------- */
SET IDENTITY_INSERT departments ON;
INSERT INTO departments (id, name) VALUES
(1, N'Kỹ thuật'),
(2, N'Nhân sự'),
(3, N'Kinh doanh'),
(4, N'Tài chính - Kế toán');
SET IDENTITY_INSERT departments OFF;

SET IDENTITY_INSERT skills ON;
INSERT INTO skills (id, name) VALUES
(1,  N'Java'),
(2,  N'Spring Boot'),
(3,  N'SQL Server'),
(4,  N'HTML/CSS'),
(5,  N'JavaScript'),
(6,  N'Kiểm thử phần mềm'),
(7,  N'Tuyển dụng'),
(8,  N'Giao tiếp'),
(9,  N'Tiếng Anh'),
(10, N'Bán hàng'),
(11, N'Kế toán'),
(12, N'Thiết kế UI/UX');
SET IDENTITY_INSERT skills OFF;

/* ---------- 5. Tin tuyển dụng: 1 DRAFT, 4 ACTIVE (của cả 2 HR), 1 CLOSED ---------- */
SET IDENTITY_INSERT jobs ON;
INSERT INTO jobs (id, title, description, location, salary_min, salary_max, deadline, status, published_at,
                  department_id, owner_id, is_deleted, deleted_at, created_at, updated_at, created_by, updated_by) VALUES
(1, N'Lập trình viên Java',
    N'Phát triển và bảo trì các dịch vụ backend bằng Java, Spring Boot. Làm việc cùng nhóm sản phẩm để thiết kế REST API và tối ưu truy vấn SQL Server.',
    N'Hà Nội', 15000000, 30000000, CAST(DATEADD(DAY, 30, @today) AS DATE), 'ACTIVE', DATEADD(DAY, -20, @now),
    1, 2, 0, NULL, DATEADD(DAY, -22, @now), DATEADD(DAY, -20, @now), 'hr1', 'hr1'),
(2, N'Lập trình viên Front-end (ReactJS)',
    N'Xây dựng giao diện web bằng ReactJS, phối hợp backend định nghĩa API và đảm bảo hiệu năng, khả năng truy cập.',
    N'TP. Hồ Chí Minh', 14000000, 28000000, CAST(DATEADD(DAY, 25, @today) AS DATE), 'ACTIVE', DATEADD(DAY, -18, @now),
    1, 3, 0, NULL, DATEADD(DAY, -19, @now), DATEADD(DAY, -18, @now), 'hr2', 'hr2'),
(3, N'Kiểm thử viên tự động hóa',
    N'Viết và duy trì kịch bản kiểm thử tự động cho web và API, tích hợp kiểm thử hồi quy vào CI/CD.',
    N'Hà Nội', 12000000, 22000000, CAST(DATEADD(DAY, -3, @today) AS DATE), 'CLOSED', DATEADD(DAY, -40, @now),
    1, 2, 0, NULL, DATEADD(DAY, -42, @now), DATEADD(DAY, -2, @now), 'hr1', 'system'),
(4, N'Chuyên viên kinh doanh B2B',
    N'Tìm kiếm và chăm sóc khách hàng doanh nghiệp, đàm phán hợp đồng, đạt chỉ tiêu doanh số theo quý.',
    N'TP. Hồ Chí Minh', 12000000, 25000000, CAST(DATEADD(DAY, 14, @today) AS DATE), 'ACTIVE', DATEADD(DAY, -12, @now),
    3, 3, 0, NULL, DATEADD(DAY, -13, @now), DATEADD(DAY, -12, @now), 'hr2', 'hr2'),
(5, N'Kế toán tổng hợp',
    N'Lập báo cáo tài chính, quyết toán thuế, làm việc với kiểm toán độc lập và hỗ trợ báo cáo quản trị cho Ban Giám đốc.',
    N'Hà Nội', 13000000, 20000000, CAST(DATEADD(DAY, 20, @today) AS DATE), 'ACTIVE', DATEADD(DAY, -10, @now),
    4, 3, 0, NULL, DATEADD(DAY, -11, @now), DATEADD(DAY, -10, @now), 'hr2', 'hr2'),
(6, N'Thiết kế UI/UX',
    N'Bản nháp: thiết kế giao diện và trải nghiệm cho ứng dụng web và mobile nội bộ.',
    N'TP. Hồ Chí Minh', NULL, NULL, CAST(DATEADD(DAY, 45, @today) AS DATE), 'DRAFT', NULL,
    1, 2, 0, NULL, DATEADD(DAY, -3, @now), NULL, 'hr1', NULL);
SET IDENTITY_INSERT jobs OFF;

INSERT INTO job_skills (job_id, skill_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 4), (2, 5),
(3, 6), (3, 3),
(4, 10), (4, 8), (4, 9),
(5, 11),
(6, 12), (6, 4);

/* ---------- 6. Hồ sơ: 8 hồ sơ phủ đủ 5 trạng thái, mỗi ứng viên nộp đúng CV của mình ----------
   Đường dẫn CV 'seed/<tên file>' trỏ tới file trong src/main/resources/static/sample/cv
   (LocalFileStorageService tự đọc từ đó khi không thấy file trong uploads/cv).

   #1 APPLIED   · Khoa → Java            (HR1 thử Duyệt)
   #2 APPLIED   · Huy  → Java            (HR1 thử Loại: CV thực tập sinh nộp vào vị trí 4 năm kinh nghiệm)
   #3 INTERVIEW · Hà   → Front-end       (vòng 1 đã qua giờ, chưa đánh giá — interviewer2 thử UC-25)
   #4 WITHDRAWN · Long → Front-end
   #5 HIRED     · Bảo  → Kiểm thử (tin đã đóng), 2 vòng đã đánh giá
   #6 INTERVIEW · Ngọc Anh → Kinh doanh  (vòng 1 đã đánh giá, vòng 2 sắp tới → HR2 chưa Tuyển được)
   #7 INTERVIEW · Hương → Kế toán        (mọi vòng đã đánh giá → HR2 thử Tuyển / Loại)
   #8 REJECTED  · Huy  → Kiểm thử, bị loại sau vòng 1                                                  */
SET IDENTITY_INSERT applications ON;
INSERT INTO applications (id, job_id, candidate_id, cv_path, cv_original_name, cover_letter, status, status_changed_at,
                          created_at, updated_at, created_by, updated_by) VALUES
(1, 1, 6,  'seed/sample_cv_java_backend.pdf',        N'NguyenMinhKhoa_CV.pdf',
   N'Tôi có 4 năm kinh nghiệm Java, Spring Boot và SQL Server, mong được trao đổi thêm về vị trí này.',
   'APPLIED',   DATEADD(DAY, -2, @now),  DATEADD(DAY, -2, @now),  NULL, 'candidate1', NULL),
(2, 1, 12, 'seed/sample_cv_fresher_intern.pdf',      N'HoangGiaHuy_CV.pdf',
   N'Em là sinh viên năm cuối, đã làm đồ án Spring Boot và mong có cơ hội làm việc thực tế.',
   'APPLIED',   DATEADD(DAY, -1, @now),  DATEADD(DAY, -1, @now),  NULL, 'candidate7', NULL),
(3, 2, 7,  'seed/sample_cv_frontend_react.pdf',      N'TranThuHa_CV.pdf',
   N'Tôi có 3 năm làm ReactJS/TypeScript và từng xây dựng design system dùng chung cho 4 sản phẩm.',
   'INTERVIEW', DATEADD(DAY, -4, @now),  DATEADD(DAY, -7, @now),  DATEADD(DAY, -4, @now), 'candidate2', 'hr2'),
(4, 2, 11, 'seed/sample_cv_uiux_designer.pdf',       N'VuHoangLong_CV.pdf',
   NULL,
   'WITHDRAWN', DATEADD(DAY, -5, @now),  DATEADD(DAY, -9, @now),  DATEADD(DAY, -5, @now), 'candidate6', 'candidate6'),
(5, 3, 8,  'seed/sample_cv_qa_automation.pdf',       N'LeQuocBao_CV.pdf',
   N'Tôi có 2 năm xây dựng kiểm thử tự động bằng Playwright và RestAssured, tích hợp vào Jenkins.',
   'HIRED',     DATEADD(DAY, -15, @now), DATEADD(DAY, -33, @now), DATEADD(DAY, -15, @now), 'candidate3', 'hr1'),
(6, 4, 9,  'seed/sample_cv_business_development.pdf', N'PhamNgocAnh_CV.pdf',
   N'Tôi vượt chỉ tiêu doanh số 3 năm liên tiếp ở mảng SaaS B2B và có mạng lưới khách hàng SME.',
   'INTERVIEW', DATEADD(DAY, -6, @now),  DATEADD(DAY, -9, @now),  DATEADD(DAY, -6, @now), 'candidate4', 'hr2'),
(7, 5, 10, 'seed/sample_cv_accountant.pdf',          N'DoThanhHuong_CV.pdf',
   NULL,
   'INTERVIEW', DATEADD(DAY, -5, @now),  DATEADD(DAY, -8, @now),  DATEADD(DAY, -5, @now), 'candidate5', 'hr2'),
(8, 3, 12, 'seed/sample_cv_fresher_intern.pdf',      N'HoangGiaHuy_CV.pdf',
   NULL,
   'REJECTED',  DATEADD(DAY, -24, @now), DATEADD(DAY, -30, @now), DATEADD(DAY, -24, @now), 'candidate7', 'hr1');
SET IDENTITY_INSERT applications OFF;

/* ---------- 7. Phỏng vấn: 7 buổi ----------
   Chưa đánh giá, đã qua giờ (demo UC-25): #1 (interviewer2, hồ sơ #3)
   Sắp tới (demo UC-24):                    #5 (interviewer1, hồ sơ #6 vòng 2)
   Đã đánh giá: #2, #3, #4, #6, #7. HR tự phỏng vấn: #3 (hr1), #6 (hr2).                        */
SET IDENTITY_INSERT interviews ON;
INSERT INTO interviews (id, application_id, interviewer_id, round_no, scheduled_at, location, status, rating, feedback, evaluated_at,
                        created_at, updated_at, created_by, updated_by) VALUES
(1, 3, 5, 1, DATEADD(HOUR, 10, DATEADD(DAY, -1,  @today)), N'Phòng họp 105',                     'SCHEDULED', NULL, NULL, NULL,
   DATEADD(DAY, -4, @now), NULL, 'hr2', NULL),
(2, 5, 4, 1, DATEADD(HOUR, 9,  DATEADD(DAY, -28, @today)), N'Phòng họp 301',                     'EVALUATED', 4,
   N'Nắm vững Selenium và Playwright, viết kịch bản gọn; cần thêm kinh nghiệm kiểm thử hiệu năng.', DATEADD(HOUR, 11, DATEADD(DAY, -28, @today)),
   DATEADD(DAY, -30, @now), DATEADD(DAY, -28, @now), 'hr1', 'interviewer1'),
(3, 5, 2, 2, DATEADD(HOUR, 14, DATEADD(DAY, -20, @today)), N'Phòng họp 202',                     'EVALUATED', 5,
   N'Thái độ chủ động, mức lương mong muốn phù hợp, có thể nhận việc sau 2 tuần.', DATEADD(HOUR, 15, DATEADD(DAY, -20, @today)),
   DATEADD(DAY, -26, @now), DATEADD(DAY, -20, @now), 'hr1', 'hr1'),
(4, 6, 5, 1, DATEADD(HOUR, 10, DATEADD(DAY, -4,  @today)), N'https://meet.google.com/rms-demo-04', 'EVALUATED', 4,
   N'Giao tiếp tốt, hiểu quy trình bán hàng B2B; cần phỏng vấn thêm vòng nữa với trưởng phòng.', DATEADD(HOUR, 11, DATEADD(DAY, -4, @today)),
   DATEADD(DAY, -6, @now), DATEADD(DAY, -4, @now), 'hr2', 'interviewer2'),
(5, 6, 4, 2, DATEADD(HOUR, 14, DATEADD(DAY, 2,   @today)), N'Văn phòng TP. Hồ Chí Minh',         'SCHEDULED', NULL, NULL, NULL,
   DATEADD(DAY, -3, @now), NULL, 'hr2', NULL),
(6, 7, 3, 1, DATEADD(HOUR, 15, DATEADD(DAY, -3,  @today)), N'Phòng họp Kế toán',                 'EVALUATED', 5,
   N'Nắm chắc chuẩn mực VAS và quyết toán thuế, từng làm việc với kiểm toán độc lập.', DATEADD(HOUR, 16, DATEADD(DAY, -3, @today)),
   DATEADD(DAY, -5, @now), DATEADD(DAY, -3, @now), 'hr2', 'hr2'),
(7, 8, 4, 1, DATEADD(HOUR, 9,  DATEADD(DAY, -26, @today)), N'Phòng họp 301',                     'EVALUATED', 2,
   N'Chưa có kinh nghiệm kiểm thử thực tế, chưa nắm vững test case cơ bản.', DATEADD(HOUR, 10, DATEADD(DAY, -26, @today)),
   DATEADD(DAY, -28, @now), DATEADD(DAY, -26, @now), 'hr1', 'interviewer1');
SET IDENTITY_INSERT interviews OFF;

/* ---------- 8. Nhật ký mẫu (vài dòng để màn SCR-08 có dữ liệu) ---------- */
SET IDENTITY_INSERT audit_logs ON;
INSERT INTO audit_logs (id, actor, action, target_type, target_id, detail, created_at) VALUES
(1,  'admin',        'CREATE_ACCOUNT',            'USER',        2,  N'Tạo tài khoản hr1 (HR)',                     DATEADD(DAY, -50, @now)),
(2,  'admin',        'CREATE_ACCOUNT',            'USER',        4,  N'Tạo tài khoản interviewer1 (INTERVIEWER)',   DATEADD(DAY, -45, @now)),
(3,  'candidate1',   'REGISTER',                  'USER',        6,  N'Ứng viên tự đăng ký',                        DATEADD(DAY, -40, @now)),
(4,  'hr1',          'CREATE_JOB',                'JOB',         1,  N'Tạo tin "Lập trình viên Java"',              DATEADD(DAY, -22, @now)),
(5,  'hr1',          'PUBLISH_JOB',               'JOB',         1,  N'DRAFT → ACTIVE',                             DATEADD(DAY, -20, @now)),
(6,  'candidate3',   'APPLY',                     'APPLICATION', 5,  N'Nộp hồ sơ vào tin #3',                       DATEADD(DAY, -33, @now)),
(7,  'hr1',          'CHANGE_APPLICATION_STATUS', 'APPLICATION', 5,  N'APPLIED → INTERVIEW',                        DATEADD(DAY, -30, @now)),
(8,  'hr1',          'SCHEDULE_INTERVIEW',        'INTERVIEW',   2,  N'Hồ sơ #5 vòng 1: interviewer1',              DATEADD(DAY, -30, @now)),
(9,  'interviewer1', 'SUBMIT_EVALUATION',         'INTERVIEW',   2,  N'Hồ sơ #5 vòng 1: 4/5',                       DATEADD(DAY, -28, @today)),
(10, 'hr1',          'CHANGE_APPLICATION_STATUS', 'APPLICATION', 5,  N'INTERVIEW → HIRED',                          DATEADD(DAY, -15, @now)),
(11, 'candidate6',   'WITHDRAW',                  'APPLICATION', 4,  N'Ứng viên rút hồ sơ',                         DATEADD(DAY, -5, @now)),
(12, 'system',       'CLOSE_JOB',                 'JOB',         3,  N'Tự đóng tin quá hạn',                        DATEADD(DAY, -2, @now));
SET IDENTITY_INSERT audit_logs OFF;

/* ---------- 9. Đặt lại bộ đếm IDENTITY: bản ghi app tạo tiếp theo nối sau dữ liệu mẫu ---------- */
DBCC CHECKIDENT ('roles',        RESEED, 4)  WITH NO_INFOMSGS;
DBCC CHECKIDENT ('users',        RESEED, 12) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('departments',  RESEED, 4)  WITH NO_INFOMSGS;
DBCC CHECKIDENT ('skills',       RESEED, 12) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('jobs',         RESEED, 6)  WITH NO_INFOMSGS;
DBCC CHECKIDENT ('applications', RESEED, 8)  WITH NO_INFOMSGS;
DBCC CHECKIDENT ('interviews',   RESEED, 7)  WITH NO_INFOMSGS;
DBCC CHECKIDENT ('audit_logs',   RESEED, 12) WITH NO_INFOMSGS;

COMMIT TRANSACTION;
PRINT N'Đã nạp dữ liệu mẫu RMS.';
