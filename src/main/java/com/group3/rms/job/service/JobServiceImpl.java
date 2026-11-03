
package com.group3.rms.job.service;

import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.job.dto.JobCreateRequest;
import com.group3.rms.job.dto.JobResponse;
import com.group3.rms.job.dto.JobSummaryResponse;
import com.group3.rms.job.dto.JobUpdateRequest;
import com.group3.rms.job.entity.Department;
import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.job.entity.Skill;
import com.group3.rms.job.mapper.JobMapper;
import com.group3.rms.job.repository.DepartmentRepository;
import com.group3.rms.job.repository.JobRepository;
import com.group3.rms.job.repository.SkillRepository;
import com.group3.rms.job.specification.JobSpecification;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final DepartmentRepository departmentRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final JobMapper jobMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    @Override
    public Page<JobSummaryResponse> searchJobs(
            String keyword,
            JobStatus status,
            Long ownerId,
            Pageable pageable
    ) {
        Specification<Job> spec = JobSpecification.notDeleted();

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(JobSpecification.titleContains(keyword));
        }

        if (status != null) {
            spec = spec.and(JobSpecification.hasStatus(status));
        }

        if (ownerId != null) {
            spec = spec.and(JobSpecification.hasOwner(ownerId));
        }

        Page<Job> jobs = jobRepository.findAll(spec, pageable);

        return jobs.map(job -> {
            long applicationCount =
                    applicationRepository.countByJobId(job.getId());

            return jobMapper.toSummary(job, applicationCount);
        });
    }

    @Override
    public JobResponse getJobDetail(Long jobId) {
        Job job = jobRepository.findByIdAndDeletedFalse(jobId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Khong tim thay tin tuyen dung: " + jobId
                        )
                );

        return jobMapper.toResponse(job);
    }

    // UC-12: Tao tin tuyen dung.
    @Override
    @Transactional
    public JobResponse createJob(JobCreateRequest request) {
        CurrentUser currentUser = currentUserService.require();

        // Chi HR moi duoc tao tin.
        if (currentUser.role() != RoleCode.HR) {
            throw new ForbiddenException(
                    "Chi HR moi duoc phep tao tin tuyen dung."
            );
        }

        validateJobRequest(request);

        User owner = userRepository.findById(currentUser.id())
                .orElseThrow(() ->
                        new NotFoundException("Khong tim thay tai khoan HR.")
                );

        Department department = departmentRepository
                .findById(request.departmentId())
                .orElseThrow(() ->
                        new BusinessException(
                                "departmentId",
                                "Phong ban khong ton tai."
                        )
                );

        Job job = new Job();
        job.setTitle(request.title().trim());
        job.setDescription(request.description().trim());
        job.setLocation(
                request.location() == null || request.location().isBlank()
                        ? null
                        : request.location().trim()
        );
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setDeadline(request.deadline());
        job.setDepartment(department);
        job.setOwner(owner);

        // Tin moi tao luon o trang thai DRAFT.
        job.setStatus(JobStatus.DRAFT);

        // Gan danh sach ky nang.
        if (request.skillIds() != null && !request.skillIds().isEmpty()) {
            if (request.skillIds().contains(null)) {
                throw new BusinessException(
                        "skillIds",
                        "Danh sach ky nang khong hop le."
                );
            }

            var skills = skillRepository.findAllById(request.skillIds());

            if (skills.size() != request.skillIds().size()) {
                throw new BusinessException(
                        "skillIds",
                        "Mot hoac nhieu ky nang khong ton tai."
                );
            }

            job.getSkills().addAll(skills);
        }

        Job savedJob = jobRepository.save(job);

        auditLogService.log(
                AuditAction.CREATE_JOB,
                TargetType.JOB,
                savedJob.getId(),
                "HR created job: " + savedJob.getTitle()
        );

        return jobMapper.toResponse(savedJob);
    }

    private void validateJobRequest(JobCreateRequest request) {
        if (request == null) {
            throw new BusinessException("Du lieu tao tin khong hop le.");
        }

        if (request.title() == null || request.title().isBlank()) {
            throw new BusinessException(
                    "title",
                    "Tieu de tin tuyen dung khong duoc de trong."
            );
        }

        if (request.title().trim().length() > 200) {
            throw new BusinessException(
                    "title",
                    "Tieu de khong duoc vuot qua 200 ky tu."
            );
        }

        if (request.description() == null
                || request.description().isBlank()) {
            throw new BusinessException(
                    "description",
                    "Mo ta cong viec khong duoc de trong."
            );
        }

        if (request.deadline() == null) {
            throw new BusinessException(
                    "deadline",
                    "Han nop ho so khong duoc de trong."
            );
        }

        if (request.deadline().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    "deadline",
                    "Han nop ho so phai la ngay hien tai hoac tuong lai."
            );
        }

        if (request.departmentId() == null) {
            throw new BusinessException(
                    "departmentId",
                    "Vui long chon phong ban."
            );
        }

        if (request.salaryMin() != null
                && request.salaryMin().signum() < 0) {
            throw new BusinessException(
                    "salaryMin",
                    "Luong toi thieu khong duoc am."
            );
        }

        if (request.salaryMax() != null
                && request.salaryMax().signum() < 0) {
            throw new BusinessException(
                    "salaryMax",
                    "Luong toi da khong duoc am."
            );
        }

        if (request.salaryMin() != null
                && request.salaryMax() != null
                && request.salaryMin().compareTo(request.salaryMax()) > 0) {
            throw new BusinessException(
                    "salaryMin",
                    "Luong toi thieu khong duoc lon hon luong toi da."
            );
        }
    }

    //UC-13
    @Override
    @Transactional
    public JobResponse updateJob(Long jobId, JobUpdateRequest request) {
        CurrentUser currentUser = currentUserService.require();

        // Chi HR moi duoc cap nhat tin.
        if (currentUser.role() != RoleCode.HR) {
            throw new ForbiddenException(
                    "Chi HR moi duoc phep cap nhat tin tuyen dung."
            );
        }

        // Tim tin chua bi xoa mem.
        Job job = jobRepository.findByIdAndDeletedFalse(jobId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Khong tim thay tin tuyen dung: " + jobId
                        )
                );

        // Chi HR phu trach tin moi duoc sua.
        if (!job.getOwner().getId().equals(currentUser.id())) {
            throw new ForbiddenException(
                    "Ban khong co quyen cap nhat tin tuyen dung nay."
            );
        }

        // Tin CLOSED khong duoc phep sua.
        if (job.getStatus() == JobStatus.CLOSED) {
            throw new BusinessException(
                    "Khong the cap nhat tin tuyen dung da dong."
            );
        }

        if (request == null) {
            throw new BusinessException(
                    "Du lieu cap nhat tin khong hop le."
            );
        }

        // Tai su dung validation cua UC-12.
        validateJobRequest(new JobCreateRequest(
                request.title(),
                request.description(),
                request.location(),
                request.salaryMin(),
                request.salaryMax(),
                request.deadline(),
                request.departmentId(),
                request.skillIds()
        ));

        // Kiem tra phong ban.
        Department department = departmentRepository
                .findById(request.departmentId())
                .orElseThrow(() ->
                        new BusinessException(
                                "departmentId",
                                "Phong ban khong ton tai."
                        )
                );

        // Cap nhat thong tin co ban.
        job.setTitle(request.title().trim());
        job.setDescription(request.description().trim());
        job.setLocation(
                request.location() == null || request.location().isBlank()
                        ? null
                        : request.location().trim()
        );
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setDeadline(request.deadline());
        job.setDepartment(department);

        // Thay the danh sach ky nang cu bang danh sach moi.
        job.getSkills().clear();

        if (request.skillIds() != null && !request.skillIds().isEmpty()) {
            if (request.skillIds().contains(null)) {
                throw new BusinessException(
                        "skillIds",
                        "Danh sach ky nang khong hop le."
                );
            }

            var skills = skillRepository.findAllById(request.skillIds());

            if (skills.size() != request.skillIds().size()) {
                throw new BusinessException(
                        "skillIds",
                        "Mot hoac nhieu ky nang khong ton tai."
                );
            }

            job.getSkills().addAll(skills);
        }

        Job updatedJob = jobRepository.save(job);

        // Ghi audit log.
        auditLogService.log(
                AuditAction.UPDATE_JOB,
                TargetType.JOB,
                updatedJob.getId(),
                "HR updated job: " + updatedJob.getTitle()
        );

        return jobMapper.toResponse(updatedJob);
    }

}
