package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationCreateRequest;
import com.group3.rms.application.dto.ApplyFormResponse;
import com.group3.rms.application.dto.MyApplicationListResponse;
import com.group3.rms.application.entity.ApplicationStatus;

/** Hồ sơ phía Candidate: nộp, theo dõi, rút (UC-18, UC-19, UC-20). */
public interface CandidateApplicationService {

    /** UC-18: dữ liệu chỉ đọc của form nộp hồ sơ; kiểm tra điều kiện ứng tuyển trước khi mở form. */
    ApplyFormResponse getApplyForm(Long jobId);

    /** UC-18: lưu CV và tạo hồ sơ APPLIED, trả về id hồ sơ. */
    Long apply(Long jobId, ApplicationCreateRequest request);

    /** UC-19: hồ sơ của người đang đăng nhập; {@code status} null là tất cả. */
    MyApplicationListResponse getMyApplications(ApplicationStatus status);

    /** UC-20: rút hồ sơ của chính mình, APPLIED / INTERVIEW → WITHDRAWN. */
    void withdraw(Long applicationId);
}
