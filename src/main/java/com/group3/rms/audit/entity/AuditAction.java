package com.group3.rms.audit.entity;

/** Các hành động được ghi nhật ký — xem docs/database.md mục 3. */
public enum AuditAction {
    REGISTER,
    CREATE_ACCOUNT,
    LOCK_ACCOUNT,
    UNLOCK_ACCOUNT,
    CHANGE_ROLE,
    CREATE_JOB,
    UPDATE_JOB,
    PUBLISH_JOB,
    CLOSE_JOB,
    DELETE_JOB,
    APPLY,
    WITHDRAW,
    CHANGE_APPLICATION_STATUS,
    SCHEDULE_INTERVIEW,
    SUBMIT_EVALUATION
}
