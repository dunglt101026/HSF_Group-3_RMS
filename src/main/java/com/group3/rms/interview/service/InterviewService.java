package com.group3.rms.interview.service;

import com.group3.rms.interview.dto.InterviewCreateRequest;
import com.group3.rms.interview.dto.InterviewEvaluationRequest;
import com.group3.rms.interview.dto.InterviewEvaluationResponse;
import com.group3.rms.interview.dto.InterviewScheduleFormResponse;
import com.group3.rms.interview.dto.InterviewScheduledResponse;
import com.group3.rms.interview.dto.MyInterviewScheduleResponse;
import com.group3.rms.interview.dto.MyInterviewTab;

public interface InterviewService {


    InterviewScheduleFormResponse getScheduleForm(Long applicationId);


    InterviewScheduledResponse scheduleInterview(Long applicationId, InterviewCreateRequest request);


    MyInterviewScheduleResponse getMySchedule(MyInterviewTab tab);


    InterviewEvaluationResponse getEvaluation(Long interviewId);


    void submitEvaluation(Long interviewId, InterviewEvaluationRequest request);
}
