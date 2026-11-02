package com.group3.rms.application.entity;

/**
 * APPLIED → INTERVIEW → HIRED; APPLIED/INTERVIEW → REJECTED; APPLIED/INTERVIEW → WITHDRAWN (Candidate rút).
 * HIRED, REJECTED, WITHDRAWN là trạng thái kết thúc.
 */
public enum ApplicationStatus {
    APPLIED,
    INTERVIEW,
    HIRED,
    REJECTED,
    WITHDRAWN
}
