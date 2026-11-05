package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationCreateRequest;
import com.group3.rms.application.dto.ApplyFormResponse;

/** Hồ sơ phía Candidate: nộp hồ sơ (UC-18). */
public interface CandidateApplicationService {

    /** UC-18: dữ liệu chỉ đọc của form nộp hồ sơ; kiểm tra điều kiện ứng tuyển trước khi mở form. */
    ApplyFormResponse getApplyForm(Long jobId);

    /** UC-18: lưu CV và tạo hồ sơ APPLIED, trả về id hồ sơ. */
    Long apply(Long jobId, ApplicationCreateRequest request);
}
