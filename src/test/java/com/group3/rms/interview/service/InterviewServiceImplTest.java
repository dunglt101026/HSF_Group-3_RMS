package com.group3.rms.interview.service;

import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.interview.dto.MyInterviewScheduleResponse;
import com.group3.rms.interview.dto.MyInterviewTab;
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

import java.time.LocalDateTime;
import java.util.List;

import static com.group3.rms.interview.InterviewTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock
    InterviewRepository interviewRepository;

    @Mock
    CurrentUserService currentUserService;

    InterviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InterviewServiceImpl(interviewRepository, new InterviewMapperImpl(), currentUserService);
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
}
