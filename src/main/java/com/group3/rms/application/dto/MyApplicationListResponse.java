package com.group3.rms.application.dto;

import com.group3.rms.application.entity.ApplicationStatus;

import java.util.List;
import java.util.Map;

/**
 * Dữ liệu SCR-16.
 *
 * @param selectedStatus tab đang chọn; null là tab "Tất cả"
 * @param applications   các hồ sơ của tab đang chọn
 * @param totalCount     tổng số hồ sơ đã nộp
 * @param statusCounts   số hồ sơ theo từng trạng thái, để hiện trên tab
 */
public record MyApplicationListResponse(
        ApplicationStatus selectedStatus,
        List<MyApplicationResponse> applications,
        long totalCount,
        Map<ApplicationStatus, Long> statusCounts
) {
}
