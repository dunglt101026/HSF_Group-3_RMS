
package com.group3.rms.job.service;

import com.group3.rms.job.dto.JobCreateRequest;
import com.group3.rms.job.dto.JobResponse;
import com.group3.rms.job.dto.JobSummaryResponse;
import com.group3.rms.job.dto.JobUpdateRequest;
import com.group3.rms.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobService {

    Page<JobSummaryResponse> searchJobs(
            String keyword,
            JobStatus status,
            Long ownerId,
            Pageable pageable
    );

    JobResponse getJobDetail(Long jobId);

    JobResponse createJob(JobCreateRequest request);

    JobResponse updateJob(Long jobId, JobUpdateRequest request);

    JobResponse publishJob(Long jobId);

    JobResponse closeJob(Long jobId);

    void deleteJob(Long jobId);
}
