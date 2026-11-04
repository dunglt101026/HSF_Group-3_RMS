package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationDetailResponse;
import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.application.mapper.ApplicationMapperImpl;
import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUserService;
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
import java.util.Optional;

import static com.group3.rms.interview.InterviewTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationReviewServiceImplTest {

    @Mock
    ApplicationRepository applicationRepository;

    @Mock
    InterviewRepository interviewRepository;

    @Mock
    CurrentUserService currentUserService;

    ApplicationReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApplicationReviewServiceImpl(
                applicationRepository,
                interviewRepository,
                new ApplicationMapperImpl(),
                new InterviewMapperImpl(),
                currentUserService
        );
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
}
