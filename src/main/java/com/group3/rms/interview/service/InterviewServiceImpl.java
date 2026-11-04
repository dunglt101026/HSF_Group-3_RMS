package com.group3.rms.interview.service;

import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.interview.dto.MyInterviewResponse;
import com.group3.rms.interview.dto.MyInterviewScheduleResponse;
import com.group3.rms.interview.dto.MyInterviewTab;
import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.entity.InterviewStatus;
import com.group3.rms.interview.mapper.InterviewMapper;
import com.group3.rms.interview.repository.InterviewRepository;
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
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewMapper interviewMapper;
    private final CurrentUserService currentUserService;

    // UC-24: Xem lịch phỏng vấn của tôi.
    @Override
    public MyInterviewScheduleResponse getMySchedule(MyInterviewTab tab) {
        CurrentUser user = currentUserService.require();
        if (user.role() != RoleCode.HR && user.role() != RoleCode.INTERVIEWER) {
            throw new ForbiddenException("Chỉ HR hoặc người phỏng vấn mới xem được lịch phỏng vấn.");
        }

        MyInterviewTab selectedTab = tab == null ? MyInterviewTab.UPCOMING : tab;
        LocalDateTime now = LocalDateTime.now();
        Long userId = user.id();

        List<Interview> interviews = switch (selectedTab) {
            case UPCOMING -> interviewRepository
                    .findByInterviewerIdAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
                            userId, InterviewStatus.SCHEDULED, now);
            case PENDING -> interviewRepository
                    .findByInterviewerIdAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                            userId, InterviewStatus.SCHEDULED, now);
            case EVALUATED -> interviewRepository
                    .findByInterviewerIdAndStatusOrderByScheduledAtDesc(userId, InterviewStatus.EVALUATED);
        };

        List<MyInterviewResponse> rows = interviews.stream()
                .map(interview -> interviewMapper.toMyInterview(
                        interview, InterviewRules.canEvaluate(interview, userId, now)))
                .toList();

        return new MyInterviewScheduleResponse(
                selectedTab,
                rows,
                interviewRepository.countByInterviewerIdAndStatusAndScheduledAtAfter(
                        userId, InterviewStatus.SCHEDULED, now),
                interviewRepository.countByInterviewerIdAndStatusAndScheduledAtLessThanEqual(
                        userId, InterviewStatus.SCHEDULED, now),
                interviewRepository.countByInterviewerIdAndStatus(userId, InterviewStatus.EVALUATED)
        );
    }
}
