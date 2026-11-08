package com.group3.rms.interview.service;

import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.interview.dto.InterviewCreateRequest;
import com.group3.rms.interview.dto.InterviewEvaluationRequest;
import com.group3.rms.interview.dto.InterviewEvaluationResponse;
import com.group3.rms.interview.dto.InterviewScheduleFormResponse;
import com.group3.rms.interview.dto.InterviewScheduledResponse;
import com.group3.rms.interview.dto.MyInterviewResponse;
import com.group3.rms.interview.dto.MyInterviewScheduleResponse;
import com.group3.rms.interview.dto.MyInterviewTab;
import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.entity.InterviewStatus;
import com.group3.rms.interview.mapper.InterviewMapper;
import com.group3.rms.interview.repository.InterviewRepository;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewServiceImpl implements InterviewService {


    private static final List<RoleCode> INTERVIEWER_ROLES = List.of(RoleCode.HR, RoleCode.INTERVIEWER);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewMapper interviewMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;


    @Override
    public InterviewScheduleFormResponse getScheduleForm(Long applicationId) {
        CurrentUser user = currentUserService.require();
        Application application = applicationRepository.findDetailById(applicationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ."));
        checkCanSchedule(application, user);

        return new InterviewScheduleFormResponse(
                application.getId(),
                application.getCandidate().getFullName(),
                application.getJob().getTitle(),
                (int) interviewRepository.countByApplicationId(applicationId) + 1,
                interviewRepository.findAssignableInterviewers(UserStatus.ACTIVE, INTERVIEWER_ROLES)
        );
    }


    @Override
    @Transactional
    public InterviewScheduledResponse scheduleInterview(Long applicationId, InterviewCreateRequest request) {
        CurrentUser user = currentUserService.require();

        Application application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ."));
        checkCanSchedule(application, user);

        LocalDateTime scheduledAt = LocalDateTime.of(request.scheduledDate(), request.scheduledTime());
        if (!scheduledAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException("scheduledDate", "Thời gian phỏng vấn phải sau thời điểm hiện tại.");
        }

        User interviewer = interviewRepository
                .findAssignableInterviewer(request.interviewerId(), UserStatus.ACTIVE, INTERVIEWER_ROLES)
                .orElseThrow(() -> new BusinessException("interviewerId",
                        "Người phỏng vấn không hợp lệ hoặc đã bị khóa."));

        int roundNo = (int) interviewRepository.countByApplicationId(applicationId) + 1;

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setInterviewer(interviewer);
        interview.setRoundNo(roundNo);
        interview.setScheduledAt(scheduledAt);
        interview.setLocation(request.location().trim());
        interview.setStatus(InterviewStatus.SCHEDULED);
        Interview saved = interviewRepository.save(interview);

        auditLogService.log(
                AuditAction.SCHEDULE_INTERVIEW,
                TargetType.INTERVIEW,
                saved.getId(),
                "Hồ sơ #" + applicationId + " vòng " + roundNo + ": " + interviewer.getUsername()
                        + " lúc " + scheduledAt.format(DATE_TIME)
        );
        log.info("Lên lịch vòng {} cho hồ sơ #{} với {}", roundNo, applicationId, interviewer.getUsername());

        return new InterviewScheduledResponse(saved.getId(), roundNo, interviewer.getFullName());
    }


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


    @Override
    public InterviewEvaluationResponse getEvaluation(Long interviewId) {
        CurrentUser user = currentUserService.require();
        Interview interview = interviewRepository.findForEvaluationById(interviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy buổi phỏng vấn."));
        checkAssigned(interview, user);


        if (interview.getStatus() != InterviewStatus.EVALUATED) {
            checkEvaluable(interview);
        }
        return interviewMapper.toEvaluation(interview);
    }


    @Override
    @Transactional
    public void submitEvaluation(Long interviewId, InterviewEvaluationRequest request) {
        CurrentUser user = currentUserService.require();
        Interview interview = interviewRepository.findByIdForUpdate(interviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy buổi phỏng vấn."));
        checkAssigned(interview, user);

        if (interview.getStatus() == InterviewStatus.EVALUATED) {
            throw new BusinessException("Buổi phỏng vấn này đã được đánh giá, không thể sửa.");
        }
        checkEvaluable(interview);

        interview.setRating(request.rating());
        interview.setFeedback(request.feedback().trim());
        interview.setEvaluatedAt(LocalDateTime.now());
        interview.setStatus(InterviewStatus.EVALUATED);
        interviewRepository.save(interview);

        auditLogService.log(
                AuditAction.SUBMIT_EVALUATION,
                TargetType.INTERVIEW,
                interview.getId(),
                "Hồ sơ #" + interview.getApplication().getId() + " vòng " + interview.getRoundNo()
                        + ": " + request.rating() + "/5"
        );
        log.info("{} gửi đánh giá buổi phỏng vấn #{}", user.username(), interview.getId());
    }


    private void checkAssigned(Interview interview, CurrentUser user) {
        if (!InterviewRules.isAssignedTo(interview, user.id())) {
            throw new ForbiddenException("Bạn không được giao buổi phỏng vấn này.");
        }
    }


    private void checkEvaluable(Interview interview) {
        if (interview.getApplication().getStatus() != ApplicationStatus.INTERVIEW) {
            throw new BusinessException("Hồ sơ không còn trong quá trình phỏng vấn.");
        }
        if (interview.getScheduledAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Chưa tới giờ phỏng vấn.");
        }
    }


    private void checkCanSchedule(Application application, CurrentUser user) {
        boolean jobOwner = user.role() == RoleCode.HR
                && user.id().equals(application.getJob().getOwner().getId());
        if (!jobOwner) {
            throw new ForbiddenException("Chỉ HR phụ trách tin mới được lên lịch phỏng vấn.");
        }
        if (application.getStatus() != ApplicationStatus.INTERVIEW) {
            throw new BusinessException("Chỉ lên lịch phỏng vấn cho hồ sơ đang ở trạng thái Phỏng vấn.");
        }
    }
}
