package com.group3.rms.interview.service;

import com.group3.rms.interview.dto.MyInterviewScheduleResponse;
import com.group3.rms.interview.dto.MyInterviewTab;

public interface InterviewService {

    /** UC-24: các buổi phỏng vấn được giao cho người đang đăng nhập, theo tab. */
    MyInterviewScheduleResponse getMySchedule(MyInterviewTab tab);
}
