package com.group3.rms.interview.service;

import com.group3.rms.interview.dto.InterviewCreateRequest;
import com.group3.rms.interview.dto.InterviewScheduleFormResponse;
import com.group3.rms.interview.dto.InterviewScheduledResponse;
import com.group3.rms.interview.dto.MyInterviewScheduleResponse;
import com.group3.rms.interview.dto.MyInterviewTab;

public interface InterviewService {

    /** UC-23: dữ liệu form lên lịch (ứng viên, vị trí, vòng tiếp theo, người phỏng vấn khả dụng). */
    InterviewScheduleFormResponse getScheduleForm(Long applicationId);

    /** UC-23: tạo buổi phỏng vấn SCHEDULED, vòng = số vòng hiện có + 1. */
    InterviewScheduledResponse scheduleInterview(Long applicationId, InterviewCreateRequest request);

    /** UC-24: các buổi phỏng vấn được giao cho người đang đăng nhập, theo tab. */
    MyInterviewScheduleResponse getMySchedule(MyInterviewTab tab);
}
