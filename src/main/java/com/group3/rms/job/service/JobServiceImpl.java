
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
import com.group3.rms.job.dto.JobResponse;
import com.group3.rms.job.dto.JobSummaryResponse;
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

}
