package com.group3.rms.job.dto;

/** Số lượng tin tuyển dụng theo trạng thái cho các tab của SCR-09. */
public record JobStatusCountsResponse(
        long all,
        long draft,
        long active,
        long closed
) {
}
