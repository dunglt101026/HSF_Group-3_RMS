package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationDetailResponse;
import com.group3.rms.application.dto.CvFileResponse;


public interface ApplicationReviewService {


    ApplicationDetailResponse getDetail(Long applicationId);


    CvFileResponse downloadCv(Long applicationId);


    void approve(Long applicationId);


    void reject(Long applicationId);


    void hire(Long applicationId);
}
