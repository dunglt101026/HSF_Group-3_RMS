package com.group3.rms.job.service;

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
import com.group3.rms.interview.repository.InterviewRepository;
import com.group3.rms.job.dto.JobApplicationRowResponse;
import com.group3.rms.job.dto.JobApplicationStatsResponse;
import com.group3.rms.job.dto.JobCreateRequest;
import com.group3.rms.job.dto.JobFormOptions;
import com.group3.rms.job.dto.JobResponse;
import com.group3.rms.job.dto.JobStatusCountsResponse;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final DepartmentRepository departmentRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final JobMapper jobMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    // UC-11: danh sách tin (mặc định "Tin tôi phụ trách" do controller truyền ownerId).
    @Override
    public Page<JobSummaryResponse> searchJobs(
            String keyword,
            JobStatus status,
            Long ownerId,
            Pageable pageable
    ) {
        requireHr();

        Specification<Job> spec = buildJobSpecification(keyword, status, ownerId);
        Page<Job> jobs = jobRepository.findAll(spec, pageable);

        // Một truy vấn cho cả trang, không đếm từng dòng (tránh N+1).
        Map<Long, Long> applicationCounts = countApplicationsByJob(
                jobs.getContent().stream().map(Job::getId).toList()
        );

        return jobs.map(job -> jobMapper.toSummary(
                job,
                applicationCounts.getOrDefault(job.getId(), 0L)
        ));
    }

    @Override
    public JobStatusCountsResponse countJobsByStatus(String keyword, Long ownerId) {
        requireHr();

        Specification<Job> base = buildJobSpecification(keyword, null, ownerId);

        return new JobStatusCountsResponse(
                jobRepository.count(base),
                jobRepository.count(base.and(JobSpecification.hasStatus(JobStatus.DRAFT))),
                jobRepository.count(base.and(JobSpecification.hasStatus(JobStatus.ACTIVE))),
                jobRepository.count(base.and(JobSpecification.hasStatus(JobStatus.CLOSED)))
        );
    }

    @Override
    public JobResponse getJobDetail(Long jobId) {
        requireHr();
        return jobMapper.toResponse(findJob(jobId));
    }

    // UC-13: form sửa chỉ mở cho HR phụ trách và tin chưa đóng.
    @Override
    public JobResponse getJobForEdit(Long jobId) {
        CurrentUser currentUser = requireHr();
        Job job = findJob(jobId);

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new BusinessException("Tin đã đóng, không thể chỉnh sửa.");
        }

        requireOwner(job, currentUser);

        return jobMapper.toResponse(job);
    }

    @Override
    public JobFormOptions getFormOptions() {
        requireHr();

        return JobFormOptions.from(
                departmentRepository.findAll(Sort.by(Sort.Direction.ASC, "name")),
                skillRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
        );
    }

    // UC-12: tạo tin ở trạng thái DRAFT.
    @Override
    @Transactional
    public JobResponse createJob(JobCreateRequest request) {
        CurrentUser currentUser = requireHr();

        if (request == null) {
            throw new BusinessException("Dữ liệu tạo tin không hợp lệ.");
        }

        validateJobFields(
                request.title(),
                request.description(),
                request.location(),
                request.salaryMin(),
                request.salaryMax(),
                request.deadline(),
                request.departmentId()
        );

        Department department = findDepartment(request.departmentId());
        Set<Skill> skills = resolveSkills(request.skillIds());

        User owner = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản HR."));

        Job job = new Job();
        applyEditableFields(
                job,
                request.title(),
                request.description(),
                request.location(),
                request.salaryMin(),
                request.salaryMax(),
                request.deadline(),
                department
        );
        job.setOwner(owner);
        job.setStatus(JobStatus.DRAFT);
        job.getSkills().addAll(skills);

        Job savedJob = jobRepository.save(job);

        auditLogService.log(
                AuditAction.CREATE_JOB,
                TargetType.JOB,
                savedJob.getId(),
                "HR đã tạo tin tuyển dụng: " + savedJob.getTitle()
        );

        return jobMapper.toResponse(savedJob);
    }

    // UC-13: cập nhật tin DRAFT / ACTIVE của chính HR phụ trách.
    @Override
    @Transactional
    public JobResponse updateJob(Long jobId, JobUpdateRequest request) {
        CurrentUser currentUser = requireHr();
        Job job = findJob(jobId);

        requireOwner(job, currentUser);

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new BusinessException("Tin đã đóng, không thể chỉnh sửa.");
        }

        if (request == null) {
            throw new BusinessException("Dữ liệu cập nhật tin không hợp lệ.");
        }

        validateJobFields(
                request.title(),
                request.description(),
                request.location(),
                request.salaryMin(),
                request.salaryMax(),
                request.deadline(),
                request.departmentId()
        );

        Department department = findDepartment(request.departmentId());
        Set<Skill> skills = resolveSkills(request.skillIds());

        applyEditableFields(
                job,
                request.title(),
                request.description(),
                request.location(),
                request.salaryMin(),
                request.salaryMax(),
                request.deadline(),
                department
        );

        job.getSkills().clear();
        job.getSkills().addAll(skills);

        Job updatedJob = jobRepository.save(job);

        auditLogService.log(
                AuditAction.UPDATE_JOB,
                TargetType.JOB,
                updatedJob.getId(),
                "HR đã cập nhật tin tuyển dụng: " + updatedJob.getTitle()
        );

        return jobMapper.toResponse(updatedJob);
    }

    // UC-14: DRAFT → ACTIVE.
    @Override
    @Transactional
    public JobResponse publishJob(Long jobId) {
        CurrentUser currentUser = requireHr();
        Job job = findJob(jobId);

        requireOwner(job, currentUser);

        if (job.getStatus() != JobStatus.DRAFT) {
            throw new BusinessException("Chỉ tin ở trạng thái Nháp mới được xuất bản.");
        }

        if (job.getDeadline().isBefore(LocalDate.now(BUSINESS_ZONE))) {
            throw new BusinessException(
                    "Hạn nộp đã qua, vui lòng sửa hạn nộp trước khi xuất bản."
            );
        }

        job.setStatus(JobStatus.ACTIVE);
        job.setPublishedAt(LocalDateTime.now(BUSINESS_ZONE));

        Job savedJob = jobRepository.save(job);

        auditLogService.log(
                AuditAction.PUBLISH_JOB,
                TargetType.JOB,
                savedJob.getId(),
                "HR đã xuất bản tin tuyển dụng: " + savedJob.getTitle()
        );

        return jobMapper.toResponse(savedJob);
    }

    // UC-14: ACTIVE → CLOSED, không mở lại được.
    @Override
    @Transactional
    public JobResponse closeJob(Long jobId) {
        CurrentUser currentUser = requireHr();
        Job job = findJob(jobId);

        requireOwner(job, currentUser);

        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new BusinessException("Chỉ tin đang tuyển mới được đóng.");
        }

        job.setStatus(JobStatus.CLOSED);

        Job savedJob = jobRepository.save(job);

        auditLogService.log(
                AuditAction.CLOSE_JOB,
                TargetType.JOB,
                savedJob.getId(),
                "HR đã đóng tin tuyển dụng: " + savedJob.getTitle()
        );

        return jobMapper.toResponse(savedJob);
    }

    // UC-14: xóa mềm, chỉ tin DRAFT chưa có hồ sơ.
    @Override
    @Transactional
    public void deleteJob(Long jobId) {
        CurrentUser currentUser = requireHr();
        Job job = findJob(jobId);

        requireOwner(job, currentUser);

        if (job.getStatus() != JobStatus.DRAFT) {
            throw new BusinessException(
                    "Chỉ có thể xóa tin tuyển dụng ở trạng thái Nháp."
            );
        }

        if (applicationRepository.countByJobId(jobId) > 0) {
            throw new BusinessException(
                    "Không thể xóa tin tuyển dụng đã có hồ sơ ứng tuyển."
            );
        }

        job.setDeleted(true);
        job.setDeletedAt(LocalDateTime.now(BUSINESS_ZONE));

        jobRepository.save(job);

        auditLogService.log(
                AuditAction.DELETE_JOB,
                TargetType.JOB,
                job.getId(),
                "HR đã xóa tin tuyển dụng: " + job.getTitle()
        );
    }

    @Override
    public JobApplicationStatsResponse getApplicationStats(Long jobId) {
        requireHr();
        findJob(jobId);

        return JobApplicationStatsResponse.from(countApplicationsByStatus(jobId));
    }

    // UC-15: mọi HR xem được hồ sơ của tin; mới nộp hiện trước.
    @Override
    public Page<JobApplicationRowResponse> searchApplications(
            Long jobId,
            ApplicationStatus status,
            Pageable pageable
    ) {
        requireHr();
        findJob(jobId);

        Specification<Application> spec = (root, query, cb) ->
                cb.equal(root.get("job").get("id"), jobId);

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<Application> page = applicationRepository.findAll(spec, pageable);

        Map<Long, Long> interviewCounts = countInterviewsByApplication(
                page.getContent().stream().map(Application::getId).toList()
        );

        return page.map(application -> JobApplicationRowResponse.from(
                application,
                interviewCounts.getOrDefault(application.getId(), 0L)
        ));
    }

    /* =====================================================================
     * Truy vấn / kiểm tra dùng chung
     * ===================================================================== */

    private Specification<Job> buildJobSpecification(String keyword, JobStatus status, Long ownerId) {
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

        return spec;
    }

    private Map<Long, Long> countApplicationsByJob(List<Long> jobIds) {
        if (jobIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new HashMap<>();
        applicationRepository.countGroupedByJobIds(jobIds)
                .forEach(row -> counts.put(row.getJobId(), row.getTotal()));

        return counts;
    }

    private Map<ApplicationStatus, Long> countApplicationsByStatus(Long jobId) {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        applicationRepository.countGroupedByStatus(jobId)
                .forEach(row -> counts.put(row.getStatus(), row.getTotal()));

        return counts;
    }

    private Map<Long, Long> countInterviewsByApplication(List<Long> applicationIds) {
        if (applicationIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new HashMap<>();
        interviewRepository.countGroupedByApplicationIds(applicationIds)
                .forEach(row -> counts.put(row.getApplicationId(), row.getTotal()));

        return counts;
    }

    private Job findJob(Long jobId) {
        return jobRepository.findByIdAndDeletedFalse(jobId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy tin tuyển dụng: " + jobId
                ));
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException(
                        "departmentId",
                        "Phòng ban không tồn tại."
                ));
    }

    private Set<Skill> resolveSkills(Set<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            throw new BusinessException(
                    "skillIds",
                    "Vui lòng chọn ít nhất một kỹ năng."
            );
        }

        // Set.of(...) là immutable: contains(null) sẽ ném NPE, nên dò qua stream.
        if (skillIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(
                    "skillIds",
                    "Danh sách kỹ năng không hợp lệ."
            );
        }

        List<Skill> skills = skillRepository.findAllById(skillIds);

        if (skills.size() != skillIds.size()) {
            throw new BusinessException(
                    "skillIds",
                    "Một hoặc nhiều kỹ năng không tồn tại."
            );
        }

        return new LinkedHashSet<>(skills);
    }

    private void applyEditableFields(
            Job job,
            String title,
            String description,
            String location,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            LocalDate deadline,
            Department department
    ) {
        job.setTitle(title.trim());
        job.setDescription(description.trim());
        job.setLocation(location == null || location.isBlank() ? null : location.trim());
        job.setSalaryMin(salaryMin);
        job.setSalaryMax(salaryMax);
        job.setDeadline(deadline);
        job.setDepartment(department);
    }

    private void validateJobFields(
            String title,
            String description,
            String location,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            LocalDate deadline,
            Long departmentId
    ) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("title", "Tiêu đề tin tuyển dụng không được để trống.");
        }

        if (title.trim().length() > 200) {
            throw new BusinessException("title", "Tiêu đề không được vượt quá 200 ký tự.");
        }

        if (description == null || description.isBlank()) {
            throw new BusinessException("description", "Mô tả công việc không được để trống.");
        }

        if (deadline == null) {
            throw new BusinessException("deadline", "Hạn nộp hồ sơ không được để trống.");
        }

        if (deadline.isBefore(LocalDate.now(BUSINESS_ZONE))) {
            throw new BusinessException(
                    "deadline",
                    "Hạn nộp hồ sơ phải là ngày hiện tại hoặc tương lai."
            );
        }

        if (location != null && location.trim().length() > 200) {
            throw new BusinessException("location", "Địa điểm làm việc không được vượt quá 200 ký tự.");
        }

        if (departmentId == null) {
            throw new BusinessException("departmentId", "Vui lòng chọn phòng ban.");
        }

        if (salaryMin != null && salaryMin.signum() < 0) {
            throw new BusinessException("salaryMin", "Lương tối thiểu không được âm.");
        }

        if (salaryMax != null && salaryMax.signum() < 0) {
            throw new BusinessException("salaryMax", "Lương tối đa không được âm.");
        }

        if (salaryMin != null
                && salaryMax != null
                && salaryMin.compareTo(salaryMax) > 0) {
            throw new BusinessException(
                    "salaryMin",
                    "Lương tối thiểu không được lớn hơn lương tối đa."
            );
        }
    }

    private CurrentUser requireHr() {
        CurrentUser currentUser = currentUserService.require();

        if (currentUser.role() != RoleCode.HR) {
            throw new ForbiddenException(
                    "Chỉ HR mới được truy cập chức năng quản lý tin tuyển dụng."
            );
        }

        return currentUser;
    }

    private void requireOwner(Job job, CurrentUser currentUser) {
        if (!job.getOwner().getId().equals(currentUser.id())) {
            throw new ForbiddenException(
                    "Bạn không có quyền thao tác trên tin tuyển dụng này."
            );
        }
    }
}
