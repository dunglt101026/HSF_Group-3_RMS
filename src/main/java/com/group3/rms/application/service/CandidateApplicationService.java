package com.group3.rms.application.service;

import com.group3.rms.application.dto.ApplicationCreateRequest;
import com.group3.rms.application.dto.ApplyFormResponse;
import com.group3.rms.application.dto.CvFileResponse;
import com.group3.rms.application.dto.MyApplicationListResponse;
import com.group3.rms.application.entity.ApplicationStatus;


public interface CandidateApplicationService {


    ApplyFormResponse getApplyForm(Long jobId);


    Long apply(Long jobId, ApplicationCreateRequest request);


    MyApplicationListResponse getMyApplications(ApplicationStatus status);


    void withdraw(Long applicationId);


    CvFileResponse getMyCv(Long applicationId);
}
