package com.group3.rms.audit.service;

import com.group3.rms.audit.dto.ActivityLogResponse;
import org.springframework.data.domain.Page;

public interface ActivityLogService {

    Page<ActivityLogResponse> searchLogs(String action, String actor, String fromDate, String toDate, String page);
}
