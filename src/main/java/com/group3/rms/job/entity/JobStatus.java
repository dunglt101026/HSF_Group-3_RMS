package com.group3.rms.job.entity;

/**
 * DRAFT → ACTIVE → CLOSED. Tin CLOSED không sửa, không mở lại.
 * Tin ACTIVE quá hạn nộp được hệ thống tự đóng lúc 00:05 hằng ngày.
 */
public enum JobStatus {
    DRAFT,
    ACTIVE,
    CLOSED
}
