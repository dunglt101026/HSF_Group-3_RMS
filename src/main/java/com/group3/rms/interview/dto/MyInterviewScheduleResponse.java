package com.group3.rms.interview.dto;

import java.util.List;


public record MyInterviewScheduleResponse(
        MyInterviewTab tab,
        List<MyInterviewResponse> interviews,
        long upcomingCount,
        long pendingCount,
        long evaluatedCount
) {

    public boolean hasAnyInterview() {
        return upcomingCount + pendingCount + evaluatedCount > 0;
    }
}
