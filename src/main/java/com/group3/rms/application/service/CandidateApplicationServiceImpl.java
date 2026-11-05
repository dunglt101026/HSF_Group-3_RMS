package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationCreateRequest;
import com.group3.rms.application.dto.ApplyFormResponse;
import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.application.mapper.MyApplicationMapper;
import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.common.storage.FileStorageService;
import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.job.repository.JobRepository;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidateApplicationServiceImpl implements CandidateApplicationService {

    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final MyApplicationMapper myApplicationMapper;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    // UC-18: Mở form nộp hồ sơ.
    @Override
    public ApplyFormResponse getApplyForm(Long jobId) {
        User candidate = requireActiveCandidate();
        Job job = findPublishedJob(jobId);
        checkCanApply(job, candidate);
        return myApplicationMapper.toApplyForm(job, candidate);
    }

    // UC-18: Nộp hồ sơ kèm CV.
    @Override
    @Transactional
    public Long apply(Long jobId, ApplicationCreateRequest request) {
        User candidate = requireActiveCandidate();
        Job job = findPublishedJob(jobId);
        checkCanApply(job, candidate);

        String cvPath = fileStorageService.store(request.cvFile());
        try {
            Application application = new Application();
            application.setJob(job);
            application.setCandidate(candidate);
            application.setCvPath(cvPath);
            application.setCvOriginalName(originalFileName(request));
            application.setCoverLetter(StringUtils.hasText(request.coverLetter()) ? request.coverLetter().trim() : null);
            application.setStatus(ApplicationStatus.APPLIED);
            application.setStatusChangedAt(LocalDateTime.now());

            // Flush để ràng buộc UNIQUE (job, candidate) được kiểm tra ngay tại đây.
            Application saved = applicationRepository.saveAndFlush(application);
            auditLogService.log(AuditAction.APPLY, TargetType.APPLICATION, saved.getId(),
                    "Nộp hồ sơ vào tin #" + job.getId());
            log.info("{} nộp hồ sơ #{} vào tin #{}", candidate.getUsername(), saved.getId(), job.getId());
            return saved.getId();
        } catch (DataIntegrityViolationException e) {
            // Hai lần nộp đồng thời: lần sau vi phạm UNIQUE.
            fileStorageService.delete(cvPath);
            throw new BusinessException("Bạn đã ứng tuyển vị trí này.");
        } catch (RuntimeException e) {
            fileStorageService.delete(cvPath);
            throw e;
        }
    }

    /** Kiểm tra bằng dữ liệu hiện tại, để tài khoản vừa bị khóa không thao tác tiếp bằng phiên cũ. */
    private User requireActiveCandidate() {
        CurrentUser current = currentUserService.require();
        if (current.role() != RoleCode.CANDIDATE) {
            throw new ForbiddenException("Chức năng này chỉ dành cho ứng viên.");
        }
        User user = userRepository.findById(current.id())
                .orElseThrow(() -> new ForbiddenException("Không tìm thấy tài khoản ứng viên."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }
        return user;
    }

    /** Tin DRAFT, đã xóa hoặc không tồn tại: coi như không tìm thấy. */
    private Job findPublishedJob(Long jobId) {
        return jobRepository.findByIdAndDeletedFalse(jobId)
                .filter(job -> job.getStatus() != JobStatus.DRAFT)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy việc làm."));
    }

    private void checkCanApply(Job job, User candidate) {
        if (applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())) {
            throw new BusinessException("Bạn đã ứng tuyển vị trí này.");
        }
        if (job.getStatus() != JobStatus.ACTIVE || job.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessException("Vị trí này đã ngừng nhận hồ sơ.");
        }
    }

    /** Bỏ phần thư mục mà một số trình duyệt gửi kèm tên file. */
    private String originalFileName(ApplicationCreateRequest request) {
        String fileName = StringUtils.getFilename(StringUtils.cleanPath(request.cvFile().getOriginalFilename()));
        if (fileName == null || fileName.length() > MAX_FILE_NAME_LENGTH) {
            throw new BusinessException("cvFile", "Tên file CV không được vượt quá 255 ký tự.");
        }
        return fileName;
    }
}
