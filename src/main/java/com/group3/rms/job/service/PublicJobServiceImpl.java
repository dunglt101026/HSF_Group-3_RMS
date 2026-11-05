package com.group3.rms.job.service;

import com.group3.rms.job.dto.PublicJobFilterResponse;
import com.group3.rms.job.dto.PublicJobSummaryResponse;
import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.job.mapper.PublicJobMapper;
import com.group3.rms.job.repository.DepartmentRepository;
import com.group3.rms.job.repository.JobRepository;
import com.group3.rms.job.specification.JobSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicJobServiceImpl implements PublicJobService {

    private static final int PAGE_SIZE = 10;

    private final JobRepository jobRepository;
    private final DepartmentRepository departmentRepository;
    private final PublicJobMapper publicJobMapper;

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

        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
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
}
