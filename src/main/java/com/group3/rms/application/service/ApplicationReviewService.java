package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationDetailResponse;

/** Xử lý hồ sơ phía HR / người phỏng vấn (UC-21, UC-22). */
public interface ApplicationReviewService {

    /** UC-21: chi tiết hồ sơ theo quyền của người đang xem. */
    ApplicationDetailResponse getDetail(Long applicationId);
}
