package com.group3.rms.job.service;

import com.group3.rms.job.dto.PublicJobDetailResponse;
import com.group3.rms.job.dto.PublicJobFilterResponse;
import com.group3.rms.job.dto.PublicJobSummaryResponse;
import org.springframework.data.domain.Page;


public interface PublicJobService {


    Page<PublicJobSummaryResponse> searchJobs(String keyword, Long departmentId, String location, int page);


    PublicJobFilterResponse getFilterOptions();


    PublicJobDetailResponse getJobDetail(Long jobId);
}
