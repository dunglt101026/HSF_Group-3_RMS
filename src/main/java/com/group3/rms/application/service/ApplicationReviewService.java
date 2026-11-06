package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationDetailResponse;
import com.group3.rms.application.dto.CvFileResponse;

/** Xử lý hồ sơ phía HR / người phỏng vấn (UC-21, UC-22). */
public interface ApplicationReviewService {

    /** UC-21: chi tiết hồ sơ theo quyền của người đang xem. */
    ApplicationDetailResponse getDetail(Long applicationId);

    /** UC-21: tải CV của hồ sơ, cùng quyền với xem chi tiết hồ sơ. */
    CvFileResponse downloadCv(Long applicationId);

    /** UC-22: duyệt hồ sơ, APPLIED → INTERVIEW. */
    void approve(Long applicationId);

    /** UC-22: loại hồ sơ, APPLIED / INTERVIEW → REJECTED. */
    void reject(Long applicationId);

    /** UC-22: tuyển, INTERVIEW → HIRED; cần ≥ 1 vòng phỏng vấn và mọi vòng đã EVALUATED. */
    void hire(Long applicationId);
}
