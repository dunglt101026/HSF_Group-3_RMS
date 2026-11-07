package com.group3.rms.job.service;

import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.job.dto.ApplyState;
import com.group3.rms.job.dto.PublicJobDetailResponse;
import com.group3.rms.job.dto.PublicJobFilterResponse;
import com.group3.rms.job.dto.PublicJobSummaryResponse;
import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.job.mapper.PublicJobMapper;
import com.group3.rms.job.repository.DepartmentRepository;
import com.group3.rms.job.repository.JobRepository;
import com.group3.rms.job.specification.JobSpecification;
import com.group3.rms.user.entity.RoleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicJobServiceImpl implements PublicJobService {

    private static final int PAGE_SIZE = 10;

    private final JobRepository jobRepository;
    private final DepartmentRepository departmentRepository;
    private final ApplicationRepository applicationRepository;
    private final PublicJobMapper publicJobMapper;
    private final CurrentUserService currentUserService;

    // UC-16: Tìm kiếm việc làm.
    @Override
    public Page<PublicJobSummaryResponse> searchJobs(String keyword, Long departmentId, String location, int page) {
        // Chỉ hiện tin ACTIVE, chưa bị xóa.
        Specification<Job> spec = JobSpecification.notDeleted()
                .and(JobSpecification.hasStatus(JobStatus.ACTIVE));

        if (StringUtils.hasText(keyword)) {
            spec = spec.and(JobSpecification.titleMatchesKeyword(keyword));
        }
        if (departmentId != null) {
            spec = spec.and(JobSpecification.hasDepartment(departmentId));
        }
        if (StringUtils.hasText(location)) {
            spec = spec.and(JobSpecification.hasLocation(location));
        }

        // Giới hạn trên để page * PAGE_SIZE không tràn số khi truy vấn.
        int safePage = Math.clamp(page, 0, Integer.MAX_VALUE / PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "publishedAt", "id"));
        return jobRepository.findAll(spec, pageable).map(publicJobMapper::toSummary);
    }

    @Override
    public PublicJobFilterResponse getFilterOptions() {
        return new PublicJobFilterResponse(
                departmentRepository.findAll(Sort.by("name")).stream()
                        .map(publicJobMapper::toDepartmentOption)
                        .toList(),
                jobRepository.findLocationsByStatus(JobStatus.ACTIVE)
        );
    }

    // UC-17: Xem chi tiết việc làm.
    @Override
    public PublicJobDetailResponse getJobDetail(Long jobId) {
        Job job = jobRepository.findByIdAndDeletedFalse(jobId)
                .filter(found -> found.getStatus() != JobStatus.DRAFT)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy việc làm."));

        return publicJobMapper.toDetail(job, resolveApplyState(job));
    }

    private ApplyState resolveApplyState(Job job) {
        Optional<CurrentUser> viewer = currentUserService.get();

        boolean candidate = viewer.isPresent() && viewer.get().role() == RoleCode.CANDIDATE;
        if (candidate && applicationRepository.existsByJobIdAndCandidateId(job.getId(), viewer.get().id())) {
            return ApplyState.ALREADY_APPLIED;
        }
        if (!isOpen(job)) {
            return ApplyState.CLOSED;
        }
        if (viewer.isEmpty()) {
            return ApplyState.LOGIN_REQUIRED;
        }
        return candidate ? ApplyState.CAN_APPLY : ApplyState.NOT_CANDIDATE;
    }

    /** Tin quá hạn nhưng lịch 00:05 chưa kịp đóng cũng coi như đã ngừng nhận hồ sơ. */
    private boolean isOpen(Job job) {
        return job.getStatus() == JobStatus.ACTIVE && !job.getDeadline().isBefore(LocalDate.now());
    }
}
