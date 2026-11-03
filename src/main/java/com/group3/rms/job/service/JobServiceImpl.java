
package com.group3.rms.job.service;

import com.group3.rms.application.repository.ApplicationRepository;
import com.group3.rms.job.dto.JobResponse;
import com.group3.rms.job.dto.JobSummaryResponse;
import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.job.mapper.JobMapper;
import com.group3.rms.job.repository.JobRepository;
import com.group3.rms.job.specification.JobSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final JobMapper jobMapper;

    @Override
    public Page<JobSummaryResponse> searchJobs(
            String keyword,
            JobStatus status,
            Long ownerId,
            Pageable pageable
    ) {
        // Luôn loại bỏ các tin đã bị xóa mềm.
        Specification<Job> spec = JobSpecification.notDeleted();

        // Chỉ thêm điều kiện tìm kiếm khi có từ khóa.
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(
                    JobSpecification.titleContains(keyword)
            );
        }

        // Lọc theo trạng thái nếu người dùng chọn.
        if (status != null) {
            spec = spec.and(
                    JobSpecification.hasStatus(status)
            );
        }

        // ownerId null: xem tất cả tin.
        // ownerId có giá trị: chỉ xem tin HR đó phụ trách.
        if (ownerId != null) {
            spec = spec.and(
                    JobSpecification.hasOwner(ownerId)
            );
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
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy tin tuyển dụng: " + jobId
                        )
                );

        return jobMapper.toResponse(job);
    }
}
