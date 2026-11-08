package com.group3.rms.application.dto;

import java.time.LocalDateTime;


public record UpcomingInterviewResponse(
        Long applicationId,
        Integer roundNo,
        LocalDateTime scheduledAt,
        String location
) {
}
