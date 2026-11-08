package com.group3.rms.job.dto;


public record JobStatusCountsResponse(
        long all,
        long draft,
        long active,
        long closed
) {
}
