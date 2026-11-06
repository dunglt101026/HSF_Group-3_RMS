package com.group3.rms.job.service;

import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.job.dto.JobApplicationRowResponse;
import com.group3.rms.job.dto.JobApplicationStatsResponse;
import com.group3.rms.job.dto.JobCreateRequest;
import com.group3.rms.job.dto.JobFormOptions;
import com.group3.rms.job.dto.JobResponse;
import com.group3.rms.job.dto.JobStatusCountsResponse;
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

    /** Số lượng tin theo trạng thái cho các tab của SCR-09. */
    JobStatusCountsResponse countJobsByStatus(String keyword, Long ownerId);

    JobResponse getJobDetail(Long jobId);

    /** Tin để mở form sửa: chỉ HR phụ trách, tin chưa đóng (UC-13). */
    JobResponse getJobForEdit(Long jobId);

    JobFormOptions getFormOptions();

    JobResponse createJob(JobCreateRequest request);

    JobResponse updateJob(Long jobId, JobUpdateRequest request);

    JobResponse publishJob(Long jobId);

    JobResponse closeJob(Long jobId);

    void deleteJob(Long jobId);

    /** Số hồ sơ theo từng trạng thái của một tin (SCR-11, SCR-12). */
    JobApplicationStatsResponse getApplicationStats(Long jobId);

    /** Danh sách hồ sơ ứng viên của một tin (UC-15). */
    Page<JobApplicationRowResponse> searchApplications(
            Long jobId,
            ApplicationStatus status,
            Pageable pageable
    );
}
