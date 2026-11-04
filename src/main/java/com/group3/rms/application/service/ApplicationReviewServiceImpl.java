package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationActions;
import com.group3.rms.application.dto.ApplicationDetailResponse;
import com.group3.rms.application.entity.Application;
import com.group3.rms.application.mapper.ApplicationMapper;
import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.interview.dto.InterviewRoundResponse;
import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.mapper.InterviewMapper;
import com.group3.rms.interview.repository.InterviewRepository;
import com.group3.rms.interview.service.InterviewRules;
import com.group3.rms.user.entity.RoleCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationReviewServiceImpl implements ApplicationReviewService {

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final ApplicationMapper applicationMapper;
    private final InterviewMapper interviewMapper;
    private final CurrentUserService currentUserService;

    // UC-21: Xem chi tiết hồ sơ.
    @Override
    public ApplicationDetailResponse getDetail(Long applicationId) {
        CurrentUser user = currentUserService.require();
        if (user.role() != RoleCode.HR && user.role() != RoleCode.INTERVIEWER) {
            throw new ForbiddenException("Bạn không có quyền xem hồ sơ này.");
        }

        Application application = applicationRepository.findDetailById(applicationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ."));
        List<Interview> interviews =
                interviewRepository.findWithInterviewerByApplicationIdOrderByRoundNoAsc(applicationId);

        boolean hrViewer = user.role() == RoleCode.HR;
        // Mọi HR xem được mọi hồ sơ; người phỏng vấn chỉ xem hồ sơ có buổi phỏng vấn của mình.
        if (!hrViewer && interviews.stream().noneMatch(i -> InterviewRules.isAssignedTo(i, user.id()))) {
            throw new ForbiddenException("Bạn không được giao phỏng vấn hồ sơ này.");
        }

        LocalDateTime now = LocalDateTime.now();
        List<InterviewRoundResponse> rounds = interviews.stream()
                .map(interview -> interviewMapper.toRound(
                        interview,
                        // HR thấy mọi đánh giá; người phỏng vấn chỉ thấy đánh giá của chính mình.
                        hrViewer || InterviewRules.isAssignedTo(interview, user.id()),
                        InterviewRules.canEvaluate(interview, user.id(), now)))
                .toList();

        boolean jobOwner = hrViewer && user.id().equals(application.getJob().getOwner().getId());
        return applicationMapper.toDetail(application, rounds, new ApplicationActions(hrViewer, jobOwner));
    }
}
