package com.group3.rms.interview.service;

import com.group3.rms.interview.dto.InterviewCreateRequest;
import com.group3.rms.interview.dto.InterviewEvaluationRequest;
import com.group3.rms.interview.dto.InterviewEvaluationResponse;
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

    /** UC-25: thông tin buổi phỏng vấn để đánh giá; đã đánh giá thì trả về để xem ở chế độ chỉ đọc. */
    InterviewEvaluationResponse getEvaluation(Long interviewId);

    /** UC-25: gửi đánh giá, buổi phỏng vấn chuyển sang EVALUATED; gửi xong không sửa được. */
    void submitEvaluation(Long interviewId, InterviewEvaluationRequest request);
}
