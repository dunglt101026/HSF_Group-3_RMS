package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationDetailResponse;
import com.group3.rms.application.dto.CvFileResponse;
import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.application.mapper.ApplicationMapperImpl;
import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.common.storage.FileStorageService;
import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.entity.InterviewStatus;
import com.group3.rms.interview.mapper.InterviewMapperImpl;
import com.group3.rms.interview.repository.InterviewRepository;
import com.group3.rms.user.entity.RoleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.group3.rms.interview.InterviewTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationReviewServiceImplTest {

    @Mock
    ApplicationRepository applicationRepository;

    @Mock
    InterviewRepository interviewRepository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    AuditLogService auditLogService;

    @Mock
    FileStorageService fileStorageService;

    ApplicationReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApplicationReviewServiceImpl(
                applicationRepository,
                interviewRepository,
                new ApplicationMapperImpl(),
                new InterviewMapperImpl(),
                currentUserService,
                auditLogService,
                fileStorageService
        );
    }

    @Nested
    @DisplayName("UC-21: Tải CV")
    class DownloadCv {

        Application application;
        Resource file;

        @BeforeEach
        void setUp() {
            application = application(1L, ApplicationStatus.INTERVIEW);
            application.setCvPath("abc.pdf");
            application.setCvOriginalName("NguyenVanAn_CV.pdf");
            file = new ByteArrayResource(new byte[]{1});
        }

        @Test
        @DisplayName("HR tải được CV với tên file gốc")
        void hrDownloads() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OTHER_ID, RoleCode.HR));
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(fileStorageService.load("abc.pdf")).thenReturn(file);

            CvFileResponse cv = service.downloadCv(1L);

            assertThat(cv.file()).isSameAs(file);
            assertThat(cv.fileName()).isEqualTo("NguyenVanAn_CV.pdf");
        }

        @Test
        @DisplayName("Người phỏng vấn được giao hồ sơ tải được CV")
        void assignedInterviewerDownloads() {
            when(currentUserService.require()).thenReturn(currentUser(INTERVIEWER_ID, RoleCode.INTERVIEWER));
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(interviewRepository.findWithInterviewerByApplicationIdOrderByRoundNoAsc(1L)).thenReturn(List.of(
                    interview(11L, application, INTERVIEWER_ID, 1, LocalDateTime.now(), InterviewStatus.SCHEDULED)));
            when(fileStorageService.load("abc.pdf")).thenReturn(file);

            assertThat(service.downloadCv(1L).file()).isSameAs(file);
        }

        @Test
        @DisplayName("Người phỏng vấn không được giao hồ sơ → ForbiddenException")
        void otherInterviewerForbidden() {
            when(currentUserService.require()).thenReturn(currentUser(INTERVIEWER_OTHER_ID, RoleCode.INTERVIEWER));
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(interviewRepository.findWithInterviewerByApplicationIdOrderByRoundNoAsc(1L)).thenReturn(List.of(
                    interview(11L, application, INTERVIEWER_ID, 1, LocalDateTime.now(), InterviewStatus.SCHEDULED)));

            assertThatThrownBy(() -> service.downloadCv(1L)).isInstanceOf(ForbiddenException.class);
            verify(fileStorageService, never()).load(anyString());
        }

        @Test
        @DisplayName("Candidate không tải được CV qua màn hình xử lý hồ sơ")
        void candidateForbidden() {
            when(currentUserService.require()).thenReturn(currentUser(CANDIDATE_ID, RoleCode.CANDIDATE));

            assertThatThrownBy(() -> service.downloadCv(1L)).isInstanceOf(ForbiddenException.class);
            verify(fileStorageService, never()).load(anyString());
        }

        @Test
        @DisplayName("Không tìm thấy hồ sơ → NotFoundException")
        void notFound() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OWNER_ID, RoleCode.HR));
            when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.downloadCv(99L)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("UC-21: Xem chi tiết hồ sơ")
    class GetDetail {

        Application application;
        Interview mine;
        Interview other;

        @BeforeEach
        void setUp() {
            application = application(1L, ApplicationStatus.INTERVIEW);
            LocalDateTime past = LocalDateTime.now().minusDays(2);
            mine = interview(11L, application, INTERVIEWER_ID, 1, past, InterviewStatus.EVALUATED);
            other = interview(12L, application, INTERVIEWER_OTHER_ID, 2, past.plusDays(1), InterviewStatus.EVALUATED);
        }

        @Test
        @DisplayName("Không tìm thấy hồ sơ → NotFoundException")
        void notFound() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OWNER_ID, RoleCode.HR));
            when(applicationRepository.findDetailById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDetail(99L)).isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Candidate không xem được màn hình xử lý hồ sơ")
        void candidateForbidden() {
            when(currentUserService.require()).thenReturn(currentUser(CANDIDATE_ID, RoleCode.CANDIDATE));

            assertThatThrownBy(() -> service.getDetail(1L)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("HR phụ trách thấy mọi đánh giá và được thao tác")
        void ownerHrSeesEverything() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OWNER_ID, RoleCode.HR));
            when(applicationRepository.findDetailById(1L)).thenReturn(Optional.of(application));
            when(interviewRepository.findWithInterviewerByApplicationIdOrderByRoundNoAsc(1L))
                    .thenReturn(List.of(mine, other));

            ApplicationDetailResponse detail = service.getDetail(1L);

            assertThat(detail.jobTitle()).isEqualTo("Lập trình viên Java");
            assertThat(detail.departmentName()).isEqualTo("Phòng Kỹ thuật");
            assertThat(detail.candidateFullName()).isEqualTo("Nguyễn Văn An");
            assertThat(detail.actions().hrViewer()).isTrue();
            assertThat(detail.actions().jobOwner()).isTrue();
            assertThat(detail.actions().canHire()).isTrue();
            assertThat(detail.actions().canReject()).isTrue();
            assertThat(detail.actions().canApprove()).isFalse();
            assertThat(detail.interviews()).allSatisfy(round -> {
                assertThat(round.evaluationVisible()).isTrue();
                assertThat(round.rating()).isEqualTo(4);
            });
        }

        @Test
        @DisplayName("HR khác xem được nhưng không phải HR phụ trách")
        void otherHrIsNotOwner() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OTHER_ID, RoleCode.HR));
            when(applicationRepository.findDetailById(1L)).thenReturn(Optional.of(application));
            when(interviewRepository.findWithInterviewerByApplicationIdOrderByRoundNoAsc(1L))
                    .thenReturn(List.of());

            ApplicationDetailResponse detail = service.getDetail(1L);

            assertThat(detail.actions().hrViewer()).isTrue();
            assertThat(detail.actions().jobOwner()).isFalse();
            assertThat(detail.actions().canReject()).isFalse();
        }

        @Test
        @DisplayName("Người phỏng vấn chỉ thấy đánh giá của chính mình")
        void interviewerSeesOnlyOwnEvaluation() {
            when(currentUserService.require()).thenReturn(currentUser(INTERVIEWER_ID, RoleCode.INTERVIEWER));
            when(applicationRepository.findDetailById(1L)).thenReturn(Optional.of(application));
            when(interviewRepository.findWithInterviewerByApplicationIdOrderByRoundNoAsc(1L))
                    .thenReturn(List.of(mine, other));

            ApplicationDetailResponse detail = service.getDetail(1L);

            assertThat(detail.actions().hrViewer()).isFalse();
            assertThat(detail.actions().jobOwner()).isFalse();
            assertThat(detail.interviews().get(0).feedback()).isEqualTo("Ứng viên tốt");
            assertThat(detail.interviews().get(1).evaluationVisible()).isFalse();
            assertThat(detail.interviews().get(1).rating()).isNull();
            assertThat(detail.interviews().get(1).feedback()).isNull();
        }

        @Test
        @DisplayName("Người phỏng vấn không được giao hồ sơ → 403")
        void interviewerNotAssignedForbidden() {
            when(currentUserService.require()).thenReturn(currentUser(INTERVIEWER_ID, RoleCode.INTERVIEWER));
            when(applicationRepository.findDetailById(1L)).thenReturn(Optional.of(application));
            when(interviewRepository.findWithInterviewerByApplicationIdOrderByRoundNoAsc(1L))
                    .thenReturn(List.of(other));

            assertThatThrownBy(() -> service.getDetail(1L)).isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("UC-22: Xử lý hồ sơ")
    class ChangeStatus {

        @BeforeEach
        void loginAsOwner() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OWNER_ID, RoleCode.HR));
        }

        @Test
        @DisplayName("Duyệt: APPLIED → INTERVIEW, ghi thời điểm đổi và nhật ký")
        void approveApplied() {
            Application application = application(1L, ApplicationStatus.APPLIED);
            when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));

            service.approve(1L);

            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
            assertThat(application.getStatusChangedAt()).isNotNull();
            verify(auditLogService).log(AuditAction.CHANGE_APPLICATION_STATUS, TargetType.APPLICATION, 1L,
                    "Hồ sơ #1: APPLIED → INTERVIEW");
        }

        @Test
        @DisplayName("Loại được cả khi APPLIED lẫn INTERVIEW")
        void rejectInterview() {
            Application application = application(1L, ApplicationStatus.INTERVIEW);
            when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));

            service.reject(1L);

            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        }

        @Test
        @DisplayName("Không phải HR phụ trách → 403, không đổi trạng thái")
        void notOwnerForbidden() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OTHER_ID, RoleCode.HR));
            Application application = application(1L, ApplicationStatus.APPLIED);
            when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> service.approve(1L)).isInstanceOf(ForbiddenException.class);
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        }

        @Test
        @DisplayName("Trạng thái kết thúc không chuyển tiếp được")
        void finishedStatusRejected() {
            Application application = application(1L, ApplicationStatus.WITHDRAWN);
            when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> service.reject(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Đã rút");
            verify(auditLogService, never()).log(any(), any(), anyLong(), anyString());
        }

        @Test
        @DisplayName("Không tuyển thẳng từ APPLIED")
        void cannotHireFromApplied() {
            Application application = application(1L, ApplicationStatus.APPLIED);
            when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> service.hire(1L)).isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Tuyển khi chưa có vòng phỏng vấn nào → lỗi")
        void hireWithoutInterview() {
            Application application = application(1L, ApplicationStatus.INTERVIEW);
            when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));
            when(interviewRepository.findByApplicationIdOrderByRoundNoAsc(1L)).thenReturn(List.of());

            assertThatThrownBy(() -> service.hire(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cần hoàn tất đánh giá tất cả các vòng phỏng vấn trước khi tuyển.");
        }

        @Test
        @DisplayName("Tuyển khi còn vòng chưa đánh giá → lỗi")
        void hireWithPendingRound() {
            Application application = application(1L, ApplicationStatus.INTERVIEW);
            LocalDateTime past = LocalDateTime.now().minusDays(1);
            when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));
            when(interviewRepository.findByApplicationIdOrderByRoundNoAsc(1L)).thenReturn(List.of(
                    interview(11L, application, INTERVIEWER_ID, 1, past, InterviewStatus.EVALUATED),
                    interview(12L, application, INTERVIEWER_ID, 2, past, InterviewStatus.SCHEDULED)));

            assertThatThrownBy(() -> service.hire(1L)).isInstanceOf(BusinessException.class);
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
        }

        @Test
        @DisplayName("Tuyển khi mọi vòng đã đánh giá → HIRED")
        void hireWhenAllEvaluated() {
            Application application = application(1L, ApplicationStatus.INTERVIEW);
            when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));
            when(interviewRepository.findByApplicationIdOrderByRoundNoAsc(1L)).thenReturn(List.of(
                    interview(11L, application, INTERVIEWER_ID, 1, LocalDateTime.now().minusDays(1),
                            InterviewStatus.EVALUATED)));

            service.hire(1L);

            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.HIRED);
        }
    }
}
