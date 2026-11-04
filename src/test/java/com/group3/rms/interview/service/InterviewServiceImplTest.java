package com.group3.rms.interview.service;

import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.interview.dto.InterviewCreateRequest;
import com.group3.rms.interview.dto.InterviewEvaluationRequest;
import com.group3.rms.interview.dto.InterviewEvaluationResponse;
import com.group3.rms.interview.dto.InterviewScheduleFormResponse;
import com.group3.rms.interview.dto.InterviewScheduledResponse;
import com.group3.rms.interview.dto.InterviewerOptionResponse;
import com.group3.rms.interview.dto.MyInterviewScheduleResponse;
import com.group3.rms.interview.dto.MyInterviewTab;
import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.entity.InterviewStatus;
import com.group3.rms.interview.mapper.InterviewMapperImpl;
import com.group3.rms.interview.repository.InterviewRepository;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.group3.rms.interview.InterviewTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock
    InterviewRepository interviewRepository;

    @Mock
    ApplicationRepository applicationRepository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    AuditLogService auditLogService;

    InterviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InterviewServiceImpl(interviewRepository, applicationRepository,
                new InterviewMapperImpl(), currentUserService, auditLogService);
    }

    @Nested
    @DisplayName("UC-24: Xem lịch phỏng vấn của tôi")
    class GetMySchedule {

        @Test
        @DisplayName("Candidate không được xem lịch phỏng vấn")
        void candidateIsForbidden() {
            when(currentUserService.require()).thenReturn(currentUser(CANDIDATE_ID, RoleCode.CANDIDATE));

            assertThatThrownBy(() -> service.getMySchedule(MyInterviewTab.UPCOMING))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Tab Cần đánh giá: chỉ bật nút Đánh giá khi hồ sơ còn INTERVIEW")
        void pendingTabMarksEvaluableRows() {
            when(currentUserService.require()).thenReturn(currentUser(INTERVIEWER_ID, RoleCode.INTERVIEWER));
            LocalDateTime past = LocalDateTime.now().minusHours(2);
            Application active = application(1L, ApplicationStatus.INTERVIEW);
            Application withdrawn = application(2L, ApplicationStatus.WITHDRAWN);
            Interview evaluable = interview(11L, active, INTERVIEWER_ID, 1, past, InterviewStatus.SCHEDULED);
            Interview closed = interview(12L, withdrawn, INTERVIEWER_ID, 1, past, InterviewStatus.SCHEDULED);

            when(interviewRepository.findByInterviewerIdAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                    eq(INTERVIEWER_ID), eq(InterviewStatus.SCHEDULED), any()))
                    .thenReturn(List.of(evaluable, closed));
            when(interviewRepository.countByInterviewerIdAndStatusAndScheduledAtLessThanEqual(
                    eq(INTERVIEWER_ID), eq(InterviewStatus.SCHEDULED), any())).thenReturn(2L);

            MyInterviewScheduleResponse result = service.getMySchedule(MyInterviewTab.PENDING);

            assertThat(result.tab()).isEqualTo(MyInterviewTab.PENDING);
            assertThat(result.pendingCount()).isEqualTo(2);
            assertThat(result.interviews()).hasSize(2);
            assertThat(result.interviews().get(0).canEvaluate()).isTrue();
            assertThat(result.interviews().get(0).candidateName()).isEqualTo("Nguyễn Văn An");
            assertThat(result.interviews().get(0).jobTitle()).isEqualTo("Lập trình viên Java");
            assertThat(result.interviews().get(1).canEvaluate()).isFalse();
        }

        @Test
        @DisplayName("Không chọn tab thì mặc định là Sắp tới; buổi chưa tới giờ không được đánh giá")
        void defaultsToUpcoming() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OWNER_ID, RoleCode.HR));
            Interview upcoming = interview(13L, application(1L, ApplicationStatus.INTERVIEW), HR_OWNER_ID, 1,
                    LocalDateTime.now().plusDays(1), InterviewStatus.SCHEDULED);
            when(interviewRepository.findByInterviewerIdAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
                    eq(HR_OWNER_ID), eq(InterviewStatus.SCHEDULED), any()))
                    .thenReturn(List.of(upcoming));

            MyInterviewScheduleResponse result = service.getMySchedule(null);

            assertThat(result.tab()).isEqualTo(MyInterviewTab.UPCOMING);
            assertThat(result.interviews()).singleElement()
                    .satisfies(row -> assertThat(row.canEvaluate()).isFalse());
        }
    }

    @Nested
    @DisplayName("UC-23: Lên lịch phỏng vấn")
    class ScheduleInterview {

        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        final InterviewCreateRequest validRequest =
                new InterviewCreateRequest(INTERVIEWER_ID, tomorrow, LocalTime.of(9, 30), "  Phòng họp 301  ");

        @Test
        @DisplayName("Form: vòng tiếp theo = số vòng hiện có + 1, kèm danh sách người phỏng vấn")
        void formData() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OWNER_ID, RoleCode.HR));
            when(applicationRepository.findDetailById(1L))
                    .thenReturn(Optional.of(application(1L, ApplicationStatus.INTERVIEW)));
            when(interviewRepository.countByApplicationId(1L)).thenReturn(2L);
            List<InterviewerOptionResponse> options =
                    List.of(new InterviewerOptionResponse(INTERVIEWER_ID, "Người phỏng vấn", RoleCode.INTERVIEWER));
            when(interviewRepository.findAssignableInterviewers(UserStatus.ACTIVE,
                    List.of(RoleCode.HR, RoleCode.INTERVIEWER))).thenReturn(options);

            InterviewScheduleFormResponse form = service.getScheduleForm(1L);

            assertThat(form.nextRoundNo()).isEqualTo(3);
            assertThat(form.candidateName()).isEqualTo("Nguyễn Văn An");
            assertThat(form.interviewers()).isEqualTo(options);
        }

        @Test
        @DisplayName("Hồ sơ không tồn tại → NotFoundException")
        void notFound() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OWNER_ID, RoleCode.HR));
            when(applicationRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.scheduleInterview(99L, validRequest))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Không phải HR phụ trách → 403")
        void notOwnerForbidden() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OTHER_ID, RoleCode.HR));
            when(applicationRepository.findByIdForUpdate(1L))
                    .thenReturn(Optional.of(application(1L, ApplicationStatus.INTERVIEW)));

            assertThatThrownBy(() -> service.scheduleInterview(1L, validRequest))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Hồ sơ chưa duyệt (APPLIED) → không lên lịch được")
        void applicationNotInInterview() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OWNER_ID, RoleCode.HR));
            when(applicationRepository.findByIdForUpdate(1L))
                    .thenReturn(Optional.of(application(1L, ApplicationStatus.APPLIED)));

            assertThatThrownBy(() -> service.scheduleInterview(1L, validRequest))
                    .isInstanceOf(BusinessException.class);
            verify(interviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thời gian không ở tương lai → lỗi gắn vào ô ngày")
        void pastTime() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OWNER_ID, RoleCode.HR));
            when(applicationRepository.findByIdForUpdate(1L))
                    .thenReturn(Optional.of(application(1L, ApplicationStatus.INTERVIEW)));
            InterviewCreateRequest past = new InterviewCreateRequest(
                    INTERVIEWER_ID, LocalDate.now().minusDays(1), LocalTime.of(9, 0), "Phòng 1");

            assertThatThrownBy(() -> service.scheduleInterview(1L, past))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Thời gian phỏng vấn phải sau thời điểm hiện tại.")
                    .extracting("field").isEqualTo("scheduledDate");
        }

        @Test
        @DisplayName("Người phỏng vấn bị khóa / sai vai trò → lỗi gắn vào ô người phỏng vấn")
        void invalidInterviewer() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OWNER_ID, RoleCode.HR));
            when(applicationRepository.findByIdForUpdate(1L))
                    .thenReturn(Optional.of(application(1L, ApplicationStatus.INTERVIEW)));
            when(interviewRepository.findAssignableInterviewer(eq(INTERVIEWER_ID), eq(UserStatus.ACTIVE), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.scheduleInterview(1L, validRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("field").isEqualTo("interviewerId");
        }

        @Test
        @DisplayName("Hợp lệ → tạo vòng mới SCHEDULED và ghi nhật ký")
        void scheduleSuccess() {
            when(currentUserService.require()).thenReturn(currentUser(HR_OWNER_ID, RoleCode.HR));
            Application application = application(1L, ApplicationStatus.INTERVIEW);
            when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));
            when(interviewRepository.findAssignableInterviewer(eq(INTERVIEWER_ID), eq(UserStatus.ACTIVE), any()))
                    .thenReturn(Optional.of(user(INTERVIEWER_ID, "Phỏng Vấn Viên")));
            when(interviewRepository.countByApplicationId(1L)).thenReturn(1L);
            when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> inv.getArgument(0));

            InterviewScheduledResponse result = service.scheduleInterview(1L, validRequest);

            ArgumentCaptor<Interview> saved = ArgumentCaptor.forClass(Interview.class);
            verify(interviewRepository).save(saved.capture());
            assertThat(saved.getValue().getRoundNo()).isEqualTo(2);
            assertThat(saved.getValue().getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
            assertThat(saved.getValue().getScheduledAt()).isEqualTo(tomorrow.atTime(9, 30));
            assertThat(saved.getValue().getLocation()).isEqualTo("Phòng họp 301");
            assertThat(result.roundNo()).isEqualTo(2);
            assertThat(result.interviewerName()).isEqualTo("Phỏng Vấn Viên");
            verify(auditLogService).log(eq(AuditAction.SCHEDULE_INTERVIEW), eq(TargetType.INTERVIEW), any(),
                    anyString());
        }
    }

    @Nested
    @DisplayName("UC-25: Gửi đánh giá phỏng vấn")
    class SubmitEvaluation {

        final InterviewEvaluationRequest request = new InterviewEvaluationRequest(4, "  Kỹ năng tốt  ");
        final LocalDateTime past = LocalDateTime.now().minusHours(1);

        @BeforeEach
        void loginAsInterviewer() {
            when(currentUserService.require()).thenReturn(currentUser(INTERVIEWER_ID, RoleCode.INTERVIEWER));
        }

        @Test
        @DisplayName("Đã tới giờ, hồ sơ còn INTERVIEW → lưu đánh giá, chuyển EVALUATED, ghi nhật ký")
        void submitSuccess() {
            Interview interview = interview(11L, application(1L, ApplicationStatus.INTERVIEW), INTERVIEWER_ID, 2,
                    past, InterviewStatus.SCHEDULED);
            when(interviewRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(interview));

            service.submitEvaluation(11L, request);

            assertThat(interview.getStatus()).isEqualTo(InterviewStatus.EVALUATED);
            assertThat(interview.getRating()).isEqualTo(4);
            assertThat(interview.getFeedback()).isEqualTo("Kỹ năng tốt");
            assertThat(interview.getEvaluatedAt()).isNotNull();
            verify(auditLogService).log(AuditAction.SUBMIT_EVALUATION, TargetType.INTERVIEW, 11L,
                    "Hồ sơ #1 vòng 2: 4/5");
        }

        @Test
        @DisplayName("Không được giao → 403")
        void notAssignedForbidden() {
            Interview interview = interview(11L, application(1L, ApplicationStatus.INTERVIEW),
                    INTERVIEWER_OTHER_ID, 1, past, InterviewStatus.SCHEDULED);
            when(interviewRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(interview));

            assertThatThrownBy(() -> service.submitEvaluation(11L, request))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Chưa tới giờ phỏng vấn → lỗi")
        void beforeScheduledTime() {
            Interview interview = interview(11L, application(1L, ApplicationStatus.INTERVIEW), INTERVIEWER_ID, 1,
                    LocalDateTime.now().plusHours(2), InterviewStatus.SCHEDULED);
            when(interviewRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(interview));

            assertThatThrownBy(() -> service.submitEvaluation(11L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Chưa tới giờ phỏng vấn.");
            assertThat(interview.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        }

        @Test
        @DisplayName("Hồ sơ đã rút → không nhận đánh giá")
        void withdrawnApplication() {
            Interview interview = interview(11L, application(1L, ApplicationStatus.WITHDRAWN), INTERVIEWER_ID, 1,
                    past, InterviewStatus.SCHEDULED);
            when(interviewRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(interview));

            assertThatThrownBy(() -> service.submitEvaluation(11L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Hồ sơ không còn trong quá trình phỏng vấn.");
        }

        @Test
        @DisplayName("Đã đánh giá → không sửa được")
        void alreadyEvaluated() {
            Interview interview = interview(11L, application(1L, ApplicationStatus.INTERVIEW), INTERVIEWER_ID, 1,
                    past, InterviewStatus.EVALUATED);
            when(interviewRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(interview));

            assertThatThrownBy(() -> service.submitEvaluation(11L, request))
                    .isInstanceOf(BusinessException.class);
            assertThat(interview.getRating()).isEqualTo(4);
            assertThat(interview.getFeedback()).isEqualTo("Ứng viên tốt");
            verify(interviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Mở lại buổi đã đánh giá → xem chỉ đọc, kể cả khi hồ sơ đã kết thúc")
        void viewEvaluatedReadOnly() {
            Interview interview = interview(11L, application(1L, ApplicationStatus.HIRED), INTERVIEWER_ID, 1,
                    past, InterviewStatus.EVALUATED);
            when(interviewRepository.findForEvaluationById(11L)).thenReturn(Optional.of(interview));

            InterviewEvaluationResponse response = service.getEvaluation(11L);

            assertThat(response.evaluated()).isTrue();
            assertThat(response.rating()).isEqualTo(4);
            assertThat(response.candidateName()).isEqualTo("Nguyễn Văn An");
        }

        @Test
        @DisplayName("Mở form khi chưa tới giờ → lỗi")
        void openFormBeforeTime() {
            Interview interview = interview(11L, application(1L, ApplicationStatus.INTERVIEW), INTERVIEWER_ID, 1,
                    LocalDateTime.now().plusDays(1), InterviewStatus.SCHEDULED);
            when(interviewRepository.findForEvaluationById(11L)).thenReturn(Optional.of(interview));

            assertThatThrownBy(() -> service.getEvaluation(11L)).isInstanceOf(BusinessException.class);
        }
    }
}
