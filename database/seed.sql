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
     candidate1 … candidate4     CANDIDATE

   Kịch bản có sẵn để demo:
     - hr1 phụ trách tin #1 (4 hồ sơ, đủ các trạng thái) và tin #4 (CLOSED).
     - interviewer2 có buổi đã qua giờ CHƯA đánh giá (UC-25): hồ sơ #5 vòng 2.
     - hr2 tự phỏng vấn, có buổi đã qua giờ chưa đánh giá: hồ sơ #8 vòng 1.
     - interviewer1, interviewer2 có buổi sắp tới: hồ sơ #2 vòng 2, hồ sơ #7 vòng 1.
     - candidate1 có hồ sơ ở 3 tin; candidate3 có hồ sơ đã rút ở tin #2 (không nộp lại được).

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
(1, 'admin',        'admin@rms.local',        @pwd, N'Quản Trị Hệ Thống', NULL,         'ACTIVE', DATEADD(DAY, -60, @now), NULL, 'system', NULL),
(2, 'hr1',          'hr1@rms.local',          @pwd, N'Trần Thu Hà',       '0901000001', 'ACTIVE', DATEADD(DAY, -50, @now), NULL, 'admin',  NULL),
(3, 'hr2',          'hr2@rms.local',          @pwd, N'Lê Minh Quân',      '0901000002', 'ACTIVE', DATEADD(DAY, -50, @now), NULL, 'admin',  NULL),
(4, 'interviewer1', 'interviewer1@rms.local', @pwd, N'Phạm Đức Anh',      '0901000003', 'ACTIVE', DATEADD(DAY, -45, @now), NULL, 'admin',  NULL),
(5, 'interviewer2', 'interviewer2@rms.local', @pwd, N'Võ Thị Mai',        '0901000004', 'ACTIVE', DATEADD(DAY, -45, @now), NULL, 'admin',  NULL),
(6, 'candidate1',   'candidate1@example.com', @pwd, N'Nguyễn Văn An',     '0912000001', 'ACTIVE', DATEADD(DAY, -30, @now), NULL, 'system', NULL),
(7, 'candidate2',   'candidate2@example.com', @pwd, N'Trần Thị Bình',     '0912000002', 'ACTIVE', DATEADD(DAY, -28, @now), NULL, 'system', NULL),
(8, 'candidate3',   'candidate3@example.com', @pwd, N'Lê Hoàng Cường',    NULL,         'ACTIVE', DATEADD(DAY, -27, @now), NULL, 'system', NULL),
(9, 'candidate4',   'candidate4@example.com', @pwd, N'Phạm Thu Dung',     '0912000004', 'ACTIVE', DATEADD(DAY, -25, @now), NULL, 'system', NULL);
SET IDENTITY_INSERT users OFF;

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),
(2, 2), (3, 2),
(4, 3), (5, 3),
(6, 4), (7, 4), (8, 4), (9, 4);

/* ---------- 4. Phòng ban, kỹ năng ---------- */
SET IDENTITY_INSERT departments ON;
INSERT INTO departments (id, name) VALUES
(1, N'Kỹ thuật'),
(2, N'Nhân sự'),
(3, N'Kinh doanh');
SET IDENTITY_INSERT departments OFF;

SET IDENTITY_INSERT skills ON;
INSERT INTO skills (id, name) VALUES
(1, N'Java'),
(2, N'Spring Boot'),
(3, N'SQL Server'),
(4, N'HTML/CSS'),
(5, N'JavaScript'),
(6, N'Kiểm thử phần mềm'),
(7, N'Tuyển dụng'),
(8, N'Giao tiếp'),
(9, N'Tiếng Anh'),
(10, N'Bán hàng');
SET IDENTITY_INSERT skills OFF;

/* ---------- 5. Tin tuyển dụng: 2 DRAFT, 3 ACTIVE (của cả 2 HR), 1 CLOSED ---------- */
SET IDENTITY_INSERT jobs ON;
INSERT INTO jobs (id, title, description, location, salary_min, salary_max, deadline, status, published_at,
                  department_id, owner_id, is_deleted, deleted_at, created_at, updated_at, created_by, updated_by) VALUES
(1, N'Lập trình viên Java',
    N'Phát triển và bảo trì các dịch vụ backend bằng Java, Spring Boot. Làm việc cùng nhóm sản phẩm để thiết kế API.',
    N'Hà Nội', 15000000, 30000000, CAST(DATEADD(DAY, 30, @today) AS DATE), 'ACTIVE', DATEADD(DAY, -20, @now),
    1, 2, 0, NULL, DATEADD(DAY, -22, @now), DATEADD(DAY, -20, @now), 'hr1', 'hr1'),
(2, N'Chuyên viên tuyển dụng',
    N'Đăng tin, sàng lọc hồ sơ, phối hợp các phòng ban tổ chức phỏng vấn.',
    N'TP. Hồ Chí Minh', 10000000, 18000000, CAST(DATEADD(DAY, 20, @today) AS DATE), 'ACTIVE', DATEADD(DAY, -15, @now),
    2, 3, 0, NULL, DATEADD(DAY, -16, @now), DATEADD(DAY, -15, @now), 'hr2', 'hr2'),
(3, N'Nhân viên kinh doanh',
    N'Tìm kiếm và chăm sóc khách hàng doanh nghiệp, đạt chỉ tiêu doanh số theo quý.',
    N'Đà Nẵng', 8000000, NULL, CAST(DATEADD(DAY, 5, @today) AS DATE), 'ACTIVE', DATEADD(DAY, -12, @now),
    3, 3, 0, NULL, DATEADD(DAY, -13, @now), DATEADD(DAY, -12, @now), 'hr2', 'hr2'),
(4, N'Kiểm thử viên phần mềm',
    N'Viết test case, kiểm thử chức năng và hồi quy cho các sản phẩm nội bộ.',
    N'Hà Nội', 9000000, 15000000, CAST(DATEADD(DAY, -3, @today) AS DATE), 'CLOSED', DATEADD(DAY, -40, @now),
    1, 2, 0, NULL, DATEADD(DAY, -42, @now), DATEADD(DAY, -2, @now), 'hr1', 'system'),
(5, N'Lập trình viên Front-end',
    N'Bản nháp: xây dựng giao diện web cho hệ thống nội bộ.',
    N'Hà Nội', NULL, NULL, CAST(DATEADD(DAY, 45, @today) AS DATE), 'DRAFT', NULL,
    1, 2, 0, NULL, DATEADD(DAY, -3, @now), NULL, 'hr1', NULL),
(6, N'Trưởng nhóm kinh doanh',
    N'Bản nháp: quản lý nhóm 5–7 nhân viên kinh doanh khu vực miền Trung.',
    N'Đà Nẵng', 20000000, 35000000, CAST(DATEADD(DAY, 60, @today) AS DATE), 'DRAFT', NULL,
    3, 3, 0, NULL, DATEADD(DAY, -1, @now), NULL, 'hr2', NULL);
SET IDENTITY_INSERT jobs OFF;

INSERT INTO job_skills (job_id, skill_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 7), (2, 8),
(3, 8), (3, 9), (3, 10),
(4, 3), (4, 6),
(5, 4), (5, 5),
(6, 9), (6, 10);

/* ---------- 6. Hồ sơ: 10 hồ sơ phủ đủ 5 trạng thái ----------
   #1 APPLIED · #2 INTERVIEW · #3 HIRED · #4 REJECTED (chưa phỏng vấn) · #5 INTERVIEW
   #6 WITHDRAWN · #7 INTERVIEW · #8 INTERVIEW · #9 REJECTED (sau phỏng vấn) · #10 HIRED        */
SET IDENTITY_INSERT applications ON;
INSERT INTO applications (id, job_id, candidate_id, cv_path, cv_original_name, cover_letter, status, status_changed_at,
                          created_at, updated_at, created_by, updated_by) VALUES
(1,  1, 6, 'seed/cv-01.pdf',  N'NguyenVanAn_CV.pdf',     N'Em có 1 năm kinh nghiệm Spring Boot, mong được trao đổi thêm.',
     'APPLIED',   DATEADD(DAY, -2, @now),  DATEADD(DAY, -2, @now),  NULL,                    'candidate1', NULL),
(2,  1, 7, 'seed/cv-02.pdf',  N'TranThiBinh_CV.pdf',     NULL,
     'INTERVIEW', DATEADD(DAY, -7, @now),  DATEADD(DAY, -10, @now), DATEADD(DAY, -7, @now),  'candidate2', 'hr1'),
(3,  1, 8, 'seed/cv-03.docx', N'LeHoangCuong_CV.docx',   N'Tôi đã làm việc 3 năm với Java và SQL Server.',
     'HIRED',     DATEADD(DAY, -3, @now),  DATEADD(DAY, -18, @now), DATEADD(DAY, -3, @now),  'candidate3', 'hr1'),
(4,  1, 9, 'seed/cv-04.pdf',  N'PhamThuDung_CV.pdf',     NULL,
     'REJECTED',  DATEADD(DAY, -9, @now),  DATEADD(DAY, -12, @now), DATEADD(DAY, -9, @now),  'candidate4', 'hr1'),
(5,  2, 6, 'seed/cv-05.pdf',  N'NguyenVanAn_CV.pdf',     NULL,
     'INTERVIEW', DATEADD(DAY, -8, @now),  DATEADD(DAY, -11, @now), DATEADD(DAY, -8, @now),  'candidate1', 'hr2'),
(6,  2, 8, 'seed/cv-06.pdf',  N'LeHoangCuong_CV.pdf',    NULL,
     'WITHDRAWN', DATEADD(DAY, -5, @now),  DATEADD(DAY, -9, @now),  DATEADD(DAY, -5, @now),  'candidate3', 'candidate3'),
(7,  3, 7, 'seed/cv-07.pdf',  N'TranThiBinh_CV.pdf',     N'Em yêu thích công việc kinh doanh và giao tiếp với khách hàng.',
     'INTERVIEW', DATEADD(DAY, -1, @now),  DATEADD(DAY, -6, @now),  DATEADD(DAY, -1, @now),  'candidate2', 'hr2'),
(8,  3, 9, 'seed/cv-08.docx', N'PhamThuDung_CV.docx',    NULL,
     'INTERVIEW', DATEADD(DAY, -4, @now),  DATEADD(DAY, -8, @now),  DATEADD(DAY, -4, @now),  'candidate4', 'hr2'),
(9,  4, 6, 'seed/cv-09.pdf',  N'NguyenVanAn_CV.pdf',     NULL,
     'REJECTED',  DATEADD(DAY, -17, @now), DATEADD(DAY, -35, @now), DATEADD(DAY, -17, @now), 'candidate1', 'hr1'),
(10, 4, 7, 'seed/cv-10.pdf',  N'TranThiBinh_CV.pdf',     NULL,
     'HIRED',     DATEADD(DAY, -15, @now), DATEADD(DAY, -33, @now), DATEADD(DAY, -15, @now), 'candidate2', 'hr1');
SET IDENTITY_INSERT applications OFF;

/* ---------- 7. Phỏng vấn ----------
   Đã đánh giá: #1, #3, #4, #5, #8, #9
   Đã qua giờ, CHƯA đánh giá (demo UC-25): #6 (interviewer2), #7 (hr2 tự phỏng vấn)
   Sắp tới: #2 (interviewer1), #10 (interviewer2)                                      */
SET IDENTITY_INSERT interviews ON;
INSERT INTO interviews (id, application_id, interviewer_id, round_no, scheduled_at, location, status, rating, feedback, evaluated_at,
                        created_at, updated_at, created_by, updated_by) VALUES
(1,  2,  4, 1, DATEADD(HOUR, 9,  DATEADD(DAY, -5,  @today)), N'Phòng họp 301',                 'EVALUATED', 4,
     N'Nền tảng Java tốt, cần thêm kinh nghiệm thiết kế hệ thống.', DATEADD(HOUR, 11, DATEADD(DAY, -5, @today)),
     DATEADD(DAY, -7, @now), DATEADD(DAY, -5, @now), 'hr1', 'interviewer1'),
(2,  2,  4, 2, DATEADD(HOUR, 14, DATEADD(DAY, 2,   @today)), N'https://meet.google.com/rms-demo-02', 'SCHEDULED', NULL, NULL, NULL,
     DATEADD(DAY, -4, @now), NULL, 'hr1', NULL),
(3,  3,  4, 1, DATEADD(HOUR, 9,  DATEADD(DAY, -12, @today)), N'Phòng họp 301',                 'EVALUATED', 5,
     N'Kỹ năng lập trình rất tốt, giải bài tập nhanh và gọn.', DATEADD(HOUR, 10, DATEADD(DAY, -12, @today)),
     DATEADD(DAY, -15, @now), DATEADD(DAY, -12, @now), 'hr1', 'interviewer1'),
(4,  3,  2, 2, DATEADD(HOUR, 10, DATEADD(DAY, -6,  @today)), N'Phòng họp 202',                 'EVALUATED', 4,
     N'Thái độ tốt, mức lương mong muốn phù hợp.', DATEADD(HOUR, 11, DATEADD(DAY, -6, @today)),
     DATEADD(DAY, -10, @now), DATEADD(DAY, -6, @now), 'hr1', 'hr1'),
(5,  5,  5, 1, DATEADD(HOUR, 10, DATEADD(DAY, -6,  @today)), N'https://meet.google.com/rms-demo-05', 'EVALUATED', 3,
     N'Giao tiếp ổn, cần phỏng vấn thêm một vòng với trưởng phòng.', DATEADD(HOUR, 12, DATEADD(DAY, -6, @today)),
     DATEADD(DAY, -8, @now), DATEADD(DAY, -6, @now), 'hr2', 'interviewer2'),
(6,  5,  5, 2, DATEADD(HOUR, 10, DATEADD(DAY, -1,  @today)), N'Phòng họp 105',                 'SCHEDULED', NULL, NULL, NULL,
     DATEADD(DAY, -5, @now), NULL, 'hr2', NULL),
(7,  8,  3, 1, DATEADD(HOUR, 15, DATEADD(DAY, -2,  @today)), N'Văn phòng Đà Nẵng',             'SCHEDULED', NULL, NULL, NULL,
     DATEADD(DAY, -4, @now), NULL, 'hr2', NULL),
(8,  9,  4, 1, DATEADD(HOUR, 9,  DATEADD(DAY, -20, @today)), N'Phòng họp 301',                 'EVALUATED', 2,
     N'Chưa nắm vững kiến thức kiểm thử cơ bản.', DATEADD(HOUR, 10, DATEADD(DAY, -20, @today)),
     DATEADD(DAY, -25, @now), DATEADD(DAY, -20, @now), 'hr1', 'interviewer1'),
(9,  10, 5, 1, DATEADD(HOUR, 14, DATEADD(DAY, -18, @today)), N'https://meet.google.com/rms-demo-09', 'EVALUATED', 5,
     N'Tư duy kiểm thử tốt, cẩn thận, phù hợp vị trí.', DATEADD(HOUR, 15, DATEADD(DAY, -18, @today)),
     DATEADD(DAY, -24, @now), DATEADD(DAY, -18, @now), 'hr1', 'interviewer2'),
(10, 7,  5, 1, DATEADD(HOUR, 15, DATEADD(DAY, 3,   @today)), N'Văn phòng Đà Nẵng',             'SCHEDULED', NULL, NULL, NULL,
     DATEADD(DAY, -1, @now), NULL, 'hr2', NULL);
SET IDENTITY_INSERT interviews OFF;

/* ---------- 8. Nhật ký mẫu (vài dòng để màn SCR-08 có dữ liệu) ---------- */
SET IDENTITY_INSERT audit_logs ON;
INSERT INTO audit_logs (id, actor, action, target_type, target_id, detail, created_at) VALUES
(1,  'admin',        'CREATE_ACCOUNT',            'USER',        2,  N'Tạo tài khoản hr1 (HR)',                     DATEADD(DAY, -50, @now)),
(2,  'admin',        'CREATE_ACCOUNT',            'USER',        4,  N'Tạo tài khoản interviewer1 (INTERVIEWER)',   DATEADD(DAY, -45, @now)),
(3,  'candidate1',   'REGISTER',                  'USER',        6,  N'Ứng viên tự đăng ký',                        DATEADD(DAY, -30, @now)),
(4,  'hr1',          'CREATE_JOB',                'JOB',         1,  N'Tạo tin "Lập trình viên Java"',              DATEADD(DAY, -22, @now)),
(5,  'hr1',          'PUBLISH_JOB',               'JOB',         1,  N'DRAFT → ACTIVE',                             DATEADD(DAY, -20, @now)),
(6,  'candidate2',   'APPLY',                     'APPLICATION', 2,  N'Nộp hồ sơ vào tin #1',                       DATEADD(DAY, -10, @now)),
(7,  'hr1',          'CHANGE_APPLICATION_STATUS', 'APPLICATION', 2,  N'APPLIED → INTERVIEW',                        DATEADD(DAY, -7, @now)),
(8,  'hr1',          'SCHEDULE_INTERVIEW',        'INTERVIEW',   1,  N'Vòng 1, người phỏng vấn interviewer1',       DATEADD(DAY, -7, @now)),
(9,  'interviewer1', 'SUBMIT_EVALUATION',         'INTERVIEW',   1,  N'Điểm 4/5',                                   DATEADD(DAY, -5, @now)),
(10, 'candidate3',   'WITHDRAW',                  'APPLICATION', 6,  N'Ứng viên rút hồ sơ',                         DATEADD(DAY, -5, @now)),
(11, 'hr1',          'CHANGE_APPLICATION_STATUS', 'APPLICATION', 3,  N'INTERVIEW → HIRED',                          DATEADD(DAY, -3, @now)),
(12, 'system',       'CLOSE_JOB',                 'JOB',         4,  N'Tự đóng tin quá hạn',                        DATEADD(DAY, -2, @now));
SET IDENTITY_INSERT audit_logs OFF;

/* ---------- 9. Đặt lại bộ đếm IDENTITY: bản ghi app tạo tiếp theo nối sau dữ liệu mẫu ---------- */
DBCC CHECKIDENT ('roles',        RESEED, 4)  WITH NO_INFOMSGS;
DBCC CHECKIDENT ('users',        RESEED, 9)  WITH NO_INFOMSGS;
DBCC CHECKIDENT ('departments',  RESEED, 3)  WITH NO_INFOMSGS;
DBCC CHECKIDENT ('skills',       RESEED, 10) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('jobs',         RESEED, 6)  WITH NO_INFOMSGS;
DBCC CHECKIDENT ('applications', RESEED, 10) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('interviews',   RESEED, 10) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('audit_logs',   RESEED, 12) WITH NO_INFOMSGS;

COMMIT TRANSACTION;
PRINT N'Đã nạp dữ liệu mẫu RMS.';
