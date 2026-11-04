package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationActions;
import com.group3.rms.application.dto.ApplicationDetailResponse;
import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.application.mapper.ApplicationMapper;
import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.interview.dto.InterviewRoundResponse;
import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.entity.InterviewStatus;
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
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationReviewServiceImpl implements ApplicationReviewService {

    /** Sơ đồ trạng thái hồ sơ phía HR (mục 4 tài liệu đặc tả). WITHDRAWN do Candidate thực hiện (UC-20). */
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> HR_TRANSITIONS = Map.of(
            ApplicationStatus.APPLIED, Set.of(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED),
            ApplicationStatus.INTERVIEW, Set.of(ApplicationStatus.HIRED, ApplicationStatus.REJECTED)
    );

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final ApplicationMapper applicationMapper;
    private final InterviewMapper interviewMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

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

        return applicationMapper.toDetail(application, rounds, buildActions(application, user));
    }

    // UC-22: Duyệt hồ sơ.
    @Override
    @Transactional
    public void approve(Long applicationId) {
        changeStatus(applicationId, ApplicationStatus.INTERVIEW);
    }

    // UC-22: Loại hồ sơ.
    @Override
    @Transactional
    public void reject(Long applicationId) {
        changeStatus(applicationId, ApplicationStatus.REJECTED);
    }

    // UC-22: Tuyển.
    @Override
    @Transactional
    public void hire(Long applicationId) {
        changeStatus(applicationId, ApplicationStatus.HIRED);
    }

    private void changeStatus(Long applicationId, ApplicationStatus target) {
        CurrentUser user = currentUserService.require();
        Application application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ."));

        if (!isJobOwner(application, user)) {
            throw new ForbiddenException("Chỉ HR phụ trách tin mới được xử lý hồ sơ này.");
        }

        ApplicationStatus current = application.getStatus();
        if (!HR_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessException("Không thể chuyển hồ sơ từ trạng thái \""
                    + label(current) + "\" sang \"" + label(target) + "\".");
        }

        if (target == ApplicationStatus.HIRED) {
            List<Interview> rounds = interviewRepository.findByApplicationIdOrderByRoundNoAsc(applicationId);
            boolean allEvaluated = !rounds.isEmpty()
                    && rounds.stream().allMatch(i -> i.getStatus() == InterviewStatus.EVALUATED);
            if (!allEvaluated) {
                throw new BusinessException("Cần hoàn tất đánh giá tất cả các vòng phỏng vấn trước khi tuyển.");
            }
        }

        application.setStatus(target);
        application.setStatusChangedAt(LocalDateTime.now());
        applicationRepository.save(application);

        auditLogService.log(
                AuditAction.CHANGE_APPLICATION_STATUS,
                TargetType.APPLICATION,
                application.getId(),
                "Hồ sơ #" + application.getId() + ": " + current + " → " + target
        );
        log.info("Hồ sơ #{} chuyển {} → {} bởi {}", application.getId(), current, target, user.username());
    }

    private ApplicationActions buildActions(Application application, CurrentUser user) {
        boolean hrViewer = user.role() == RoleCode.HR;
        boolean owner = isJobOwner(application, user);
        ApplicationStatus status = application.getStatus();
        return new ApplicationActions(
                hrViewer,
                owner,
                owner && status == ApplicationStatus.APPLIED,
                owner && (status == ApplicationStatus.APPLIED || status == ApplicationStatus.INTERVIEW),
                owner && status == ApplicationStatus.INTERVIEW,
                owner && status == ApplicationStatus.INTERVIEW
        );
    }

    private boolean isJobOwner(Application application, CurrentUser user) {
        return user.role() == RoleCode.HR && user.id().equals(application.getJob().getOwner().getId());
    }

    private String label(ApplicationStatus status) {
        return switch (status) {
            case APPLIED -> "Chờ duyệt";
            case INTERVIEW -> "Phỏng vấn";
            case HIRED -> "Đã tuyển";
            case REJECTED -> "Đã loại";
            case WITHDRAWN -> "Đã rút";
        };
    }
}
