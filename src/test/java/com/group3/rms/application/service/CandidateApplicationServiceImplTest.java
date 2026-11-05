package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationCreateRequest;
import com.group3.rms.application.dto.ApplyFormResponse;
import com.group3.rms.application.dto.MyApplicationListResponse;
import com.group3.rms.application.dto.UpcomingInterviewResponse;
import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.application.mapper.MyApplicationMapperImpl;
import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.common.storage.FileStorageService;
import com.group3.rms.interview.entity.InterviewStatus;
import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.job.repository.JobRepository;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.group3.rms.interview.InterviewTestData.CANDIDATE_ID;
import static com.group3.rms.interview.InterviewTestData.application;
import static com.group3.rms.interview.InterviewTestData.currentUser;
import static com.group3.rms.interview.InterviewTestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateApplicationServiceImplTest {

    private static final long JOB_ID = 10L;
    private static final long OTHER_CANDIDATE_ID = 7L;

    @Mock
    ApplicationRepository applicationRepository;

    @Mock
    JobRepository jobRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    FileStorageService fileStorageService;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    AuditLogService auditLogService;

    CandidateApplicationServiceImpl service;

    User candidate;

    @BeforeEach
    void setUp() {
        service = new CandidateApplicationServiceImpl(applicationRepository, jobRepository, userRepository,
                new MyApplicationMapperImpl(), fileStorageService, currentUserService, auditLogService);
        candidate = user(CANDIDATE_ID, "Nguyễn Văn An");
        candidate.setStatus(UserStatus.ACTIVE);
    }

    private void loginAsCandidate() {
        when(currentUserService.require()).thenReturn(currentUser(CANDIDATE_ID, RoleCode.CANDIDATE));
        when(userRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
    }

    private Job job(JobStatus status, LocalDate deadline) {
        Job job = new Job();
        ReflectionTestUtils.setField(job, "id", JOB_ID);
        job.setTitle("Lập trình viên Java");
        job.setStatus(status);
        job.setDeadline(deadline);
        return job;
    }

    private void jobInDb(Job job) {
        when(jobRepository.findByIdAndDeletedFalse(JOB_ID)).thenReturn(Optional.of(job));
    }

    private final LocalDate nextWeek = LocalDate.now().plusDays(7);
    private final ApplicationCreateRequest validRequest = new ApplicationCreateRequest(
            new MockMultipartFile("cvFile", "CV_An.pdf", "application/pdf", new byte[]{1, 2, 3}),
            "  Thư giới thiệu  ");

    @Nested
    @DisplayName("Quyền của người dùng")
    class Permission {

        @Test
        @DisplayName("Không phải Candidate → 403")
        void notCandidate() {
            when(currentUserService.require()).thenReturn(currentUser(2L, RoleCode.HR));

            assertThatThrownBy(() -> service.getMyApplications(null)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Tài khoản vừa bị khóa, phiên cũ → 403")
        void lockedAccount() {
            loginAsCandidate();
            candidate.setStatus(UserStatus.INACTIVE);

            assertThatThrownBy(() -> service.getMyApplications(null)).isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("UC-18: Nộp hồ sơ")
    class Apply {

        @BeforeEach
        void login() {
            loginAsCandidate();
        }

        @Test
        @DisplayName("Form hiện vị trí, họ tên, email ứng viên")
        void formData() {
            jobInDb(job(JobStatus.ACTIVE, nextWeek));

            ApplyFormResponse form = service.getApplyForm(JOB_ID);

            assertThat(form.jobId()).isEqualTo(JOB_ID);
            assertThat(form.jobTitle()).isEqualTo("Lập trình viên Java");
            assertThat(form.candidateFullName()).isEqualTo("Nguyễn Văn An");
            assertThat(form.candidateEmail()).isEqualTo(candidate.getEmail());
        }

        @Test
        @DisplayName("Hợp lệ → lưu CV, tạo hồ sơ APPLIED, ghi nhật ký APPLY")
        void applySuccess() {
            jobInDb(job(JobStatus.ACTIVE, nextWeek));
            when(fileStorageService.store(validRequest.cvFile())).thenReturn("abc.pdf");
            when(applicationRepository.saveAndFlush(any(Application.class))).thenAnswer(invocation -> {
                Application saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 50L);
                return saved;
            });

            Long id = service.apply(JOB_ID, validRequest);

            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationRepository).saveAndFlush(captor.capture());
            Application saved = captor.getValue();
            assertThat(id).isEqualTo(50L);
            assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
            assertThat(saved.getCvPath()).isEqualTo("abc.pdf");
            assertThat(saved.getCvOriginalName()).isEqualTo("CV_An.pdf");
            assertThat(saved.getCoverLetter()).isEqualTo("Thư giới thiệu");
            assertThat(saved.getCandidate()).isSameAs(candidate);
            verify(auditLogService).log(AuditAction.APPLY, TargetType.APPLICATION, 50L, "Nộp hồ sơ vào tin #10");
        }

        @Test
        @DisplayName("Thư giới thiệu để trống → lưu null")
        void blankCoverLetter() {
            jobInDb(job(JobStatus.ACTIVE, nextWeek));
            when(applicationRepository.saveAndFlush(any(Application.class)))
                    .thenAnswer(invocation -> {
                        Application saved = invocation.getArgument(0);
                        ReflectionTestUtils.setField(saved, "id", 50L);
                        return saved;
                    });

            service.apply(JOB_ID, new ApplicationCreateRequest(validRequest.cvFile(), "   "));

            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getCoverLetter()).isNull();
        }

        @Test
        @DisplayName("Đã nộp (kể cả đã rút) → báo lỗi, không lưu file")
        void alreadyApplied() {
            jobInDb(job(JobStatus.ACTIVE, nextWeek));
            when(applicationRepository.existsByJobIdAndCandidateId(JOB_ID, CANDIDATE_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.apply(JOB_ID, validRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Bạn đã ứng tuyển vị trí này.")
                    .extracting("field").isNull();
            verify(fileStorageService, never()).store(any());
        }

        @Test
        @DisplayName("Tin vừa đóng → Vị trí này đã ngừng nhận hồ sơ")
        void closedJob() {
            jobInDb(job(JobStatus.CLOSED, nextWeek));

            assertThatThrownBy(() -> service.apply(JOB_ID, validRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Vị trí này đã ngừng nhận hồ sơ.");
        }

        @Test
        @DisplayName("Tin quá hạn nhưng chưa bị tự đóng → không nhận hồ sơ")
        void expiredJob() {
            jobInDb(job(JobStatus.ACTIVE, LocalDate.now().minusDays(1)));

            assertThatThrownBy(() -> service.apply(JOB_ID, validRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Vị trí này đã ngừng nhận hồ sơ.");
        }

        @Test
        @DisplayName("Tin DRAFT → 404")
        void draftJob() {
            jobInDb(job(JobStatus.DRAFT, nextWeek));

            assertThatThrownBy(() -> service.apply(JOB_ID, validRequest)).isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Nộp đồng thời, vi phạm UNIQUE → báo đã ứng tuyển và xóa file vừa lưu")
        void concurrentDuplicate() {
            jobInDb(job(JobStatus.ACTIVE, nextWeek));
            when(fileStorageService.store(validRequest.cvFile())).thenReturn("abc.pdf");
            when(applicationRepository.saveAndFlush(any(Application.class)))
                    .thenThrow(new DataIntegrityViolationException("uq_applications_job_candidate"));

            assertThatThrownBy(() -> service.apply(JOB_ID, validRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Bạn đã ứng tuyển vị trí này.");
            verify(fileStorageService).delete("abc.pdf");
        }

        @Test
        @DisplayName("Tên file quá dài → lỗi dưới ô cvFile và xóa file vừa lưu")
        void fileNameTooLong() {
            jobInDb(job(JobStatus.ACTIVE, nextWeek));
            when(fileStorageService.store(any())).thenReturn("abc.pdf");
            ApplicationCreateRequest request = new ApplicationCreateRequest(
                    new MockMultipartFile("cvFile", "a".repeat(256) + ".pdf", "application/pdf", new byte[]{1}), null);

            assertThatThrownBy(() -> service.apply(JOB_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("field").isEqualTo("cvFile");
            verify(fileStorageService).delete("abc.pdf");
        }
    }

    @Nested
    @DisplayName("UC-19: Hồ sơ của tôi")
    class MyApplications {

        @BeforeEach
        void login() {
            loginAsCandidate();
        }

        private void applicationsInDb() {
            when(applicationRepository.findByCandidateIdOrderByCreatedAtDescIdDesc(CANDIDATE_ID)).thenReturn(List.of(
                    application(1L, ApplicationStatus.INTERVIEW),
                    application(2L, ApplicationStatus.HIRED),
                    application(3L, ApplicationStatus.INTERVIEW)));
        }

        @Test
        @DisplayName("Tab Tất cả: đủ hồ sơ, đếm theo trạng thái, gắn lịch sắp tới đúng hồ sơ")
        void allTab() {
            applicationsInDb();
            UpcomingInterviewResponse upcoming =
                    new UpcomingInterviewResponse(1L, 2, LocalDateTime.now().plusDays(1), "Phòng họp 301");
            when(applicationRepository.findUpcomingInterviews(eq(CANDIDATE_ID), eq(ApplicationStatus.INTERVIEW),
                    eq(InterviewStatus.SCHEDULED), any())).thenReturn(List.of(upcoming));

            MyApplicationListResponse result = service.getMyApplications(null);

            assertThat(result.selectedStatus()).isNull();
            assertThat(result.totalCount()).isEqualTo(3);
            assertThat(result.statusCounts().get(ApplicationStatus.INTERVIEW)).isEqualTo(2);
            assertThat(result.statusCounts().get(ApplicationStatus.WITHDRAWN)).isZero();
            assertThat(result.applications()).hasSize(3);
            assertThat(result.applications().get(0).upcomingInterviews()).containsExactly(upcoming);
            assertThat(result.applications().get(1).upcomingInterviews()).isEmpty();
            assertThat(result.applications().get(0).jobTitle()).isEqualTo("Lập trình viên Java");
        }

        @Test
        @DisplayName("Lọc theo tab: chỉ còn hồ sơ của trạng thái đó, số đếm giữ nguyên")
        void filterByStatus() {
            applicationsInDb();

            MyApplicationListResponse result = service.getMyApplications(ApplicationStatus.HIRED);

            assertThat(result.applications()).singleElement()
                    .satisfies(row -> assertThat(row.id()).isEqualTo(2L));
            assertThat(result.totalCount()).isEqualTo(3);
        }
    }
}
