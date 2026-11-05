package com.group3.rms.job.service;

import com.group3.rms.job.dto.PublicJobFilterResponse;
import com.group3.rms.job.dto.PublicJobSummaryResponse;
import org.springframework.data.domain.Page;

/** Việc làm công khai cho Guest và Candidate (UC-16). */
public interface PublicJobService {

    /** UC-16: tin ACTIVE theo bộ lọc, mới xuất bản trước, 10 tin/trang. Bộ lọc trống thì bỏ qua. */
    Page<PublicJobSummaryResponse> searchJobs(String keyword, Long departmentId, String location, int page);

    /** UC-16: các lựa chọn của bộ lọc phòng ban và địa điểm. */
    PublicJobFilterResponse getFilterOptions();
}
