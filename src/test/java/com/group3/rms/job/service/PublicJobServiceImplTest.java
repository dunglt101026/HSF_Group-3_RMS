package com.group3.rms.job.service;

import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.job.dto.ApplyState;
import com.group3.rms.job.dto.PublicJobDetailResponse;
import com.group3.rms.job.entity.Department;
import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.job.entity.Skill;
import com.group3.rms.job.mapper.PublicJobMapperImpl;
import com.group3.rms.job.repository.DepartmentRepository;
import com.group3.rms.job.repository.JobRepository;
import com.group3.rms.user.entity.RoleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicJobServiceImplTest {

    private static final long JOB_ID = 1L;
    private static final long CANDIDATE_ID = 6L;

    @Mock
    JobRepository jobRepository;

    @Mock
    DepartmentRepository departmentRepository;

    @Mock
    ApplicationRepository applicationRepository;

    @Mock
    CurrentUserService currentUserService;

    PublicJobServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublicJobServiceImpl(jobRepository, departmentRepository, applicationRepository,
                new PublicJobMapperImpl(), currentUserService);
    }

    private Job job(JobStatus status, LocalDate deadline) {
        Department department = new Department();
        department.setName("Kỹ thuật");

        Skill java = new Skill();
        java.setName("Java");

        Job job = new Job();
        ReflectionTestUtils.setField(job, "id", JOB_ID);
        job.setTitle("Lập trình viên Java");
        job.setDescription("Mô tả");
        job.setDepartment(department);
        job.setStatus(status);
        job.setDeadline(deadline);
        job.setSalaryMin(new BigDecimal("15000000"));
        job.setSalaryMax(new BigDecimal("30000000"));
        job.getSkills().add(java);
        return job;
    }

    private void jobInDb(Job job) {
        when(jobRepository.findByIdAndDeletedFalse(JOB_ID)).thenReturn(Optional.of(job));
    }

    private void loginAs(RoleCode role) {
        when(currentUserService.get()).thenReturn(Optional.of(new CurrentUser(CANDIDATE_ID, "user", "User", role)));
    }

    private void guest() {
        when(currentUserService.get()).thenReturn(Optional.empty());
    }

    private final LocalDate nextWeek = LocalDate.now().plusDays(7);

    @Test
    @DisplayName("Tin DRAFT → 404")
    void draftIsNotFound() {
        jobInDb(job(JobStatus.DRAFT, nextWeek));

        assertThatThrownBy(() -> service.getJobDetail(JOB_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Tin đã xóa hoặc không tồn tại → 404")
    void missingIsNotFound() {
        when(jobRepository.findByIdAndDeletedFalse(JOB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJobDetail(JOB_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Guest xem tin đang tuyển → Đăng nhập để ứng tuyển, kèm dữ liệu hiển thị")
    void guestMustLogin() {
        jobInDb(job(JobStatus.ACTIVE, nextWeek));
        guest();

        PublicJobDetailResponse detail = service.getJobDetail(JOB_ID);

        assertThat(detail.applyState()).isEqualTo(ApplyState.LOGIN_REQUIRED);
        assertThat(detail.departmentName()).isEqualTo("Kỹ thuật");
        assertThat(detail.skills()).containsExactly("Java");
        assertThat(detail.salaryText()).isEqualTo("15.000.000 – 30.000.000 VNĐ");
    }

    @Test
    @DisplayName("Candidate chưa nộp → được ứng tuyển")
    void candidateCanApply() {
        jobInDb(job(JobStatus.ACTIVE, nextWeek));
        loginAs(RoleCode.CANDIDATE);
        when(applicationRepository.existsByJobIdAndCandidateId(JOB_ID, CANDIDATE_ID)).thenReturn(false);

        assertThat(service.getJobDetail(JOB_ID).applyState()).isEqualTo(ApplyState.CAN_APPLY);
    }

    @Test
    @DisplayName("Candidate đã nộp, kể cả tin đã đóng → Bạn đã ứng tuyển")
    void candidateAlreadyApplied() {
        jobInDb(job(JobStatus.CLOSED, nextWeek));
        loginAs(RoleCode.CANDIDATE);
        when(applicationRepository.existsByJobIdAndCandidateId(JOB_ID, CANDIDATE_ID)).thenReturn(true);

        assertThat(service.getJobDetail(JOB_ID).applyState()).isEqualTo(ApplyState.ALREADY_APPLIED);
    }

    @Test
    @DisplayName("Tin đã đóng → ngừng nhận hồ sơ, Guest không cần đăng nhập")
    void closedJob() {
        jobInDb(job(JobStatus.CLOSED, nextWeek));
        guest();

        assertThat(service.getJobDetail(JOB_ID).applyState()).isEqualTo(ApplyState.CLOSED);
    }

    @Test
    @DisplayName("Tin ACTIVE đã quá hạn nhưng chưa tới lịch tự đóng → ngừng nhận hồ sơ")
    void expiredButStillActive() {
        jobInDb(job(JobStatus.ACTIVE, LocalDate.now().minusDays(1)));
        guest();

        assertThat(service.getJobDetail(JOB_ID).applyState()).isEqualTo(ApplyState.CLOSED);
    }

    @Test
    @DisplayName("Tin có hạn nộp là hôm nay vẫn nhận hồ sơ")
    void deadlineTodayIsOpen() {
        jobInDb(job(JobStatus.ACTIVE, LocalDate.now()));
        guest();

        assertThat(service.getJobDetail(JOB_ID).applyState()).isEqualTo(ApplyState.LOGIN_REQUIRED);
    }

    @Test
    @DisplayName("Nhân viên xem tin → không có nút ứng tuyển")
    void staffCannotApply() {
        jobInDb(job(JobStatus.ACTIVE, nextWeek));
        loginAs(RoleCode.HR);

        assertThat(service.getJobDetail(JOB_ID).applyState()).isEqualTo(ApplyState.NOT_CANDIDATE);
    }
}
