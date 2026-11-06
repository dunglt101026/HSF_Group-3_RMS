package com.group3.rms.interview.service;

import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.entity.InterviewStatus;

import java.time.LocalDateTime;

/** Quy tắc dùng chung giữa các màn của chức năng phỏng vấn (UC-21, UC-24, UC-25). */
public final class InterviewRules {

    private InterviewRules() {
    }

    /**
     * Người dùng được gửi đánh giá khi: là người được giao, buổi còn SCHEDULED,
     * đã tới giờ hẹn và hồ sơ vẫn đang INTERVIEW (đã rút / kết thúc thì không nhận đánh giá).
     */
    public static boolean canEvaluate(Interview interview, Long userId, LocalDateTime now) {
        return isAssignedTo(interview, userId)
                && interview.getStatus() == InterviewStatus.SCHEDULED
                && !interview.getScheduledAt().isAfter(now)
                && interview.getApplication().getStatus() == ApplicationStatus.INTERVIEW;
    }

    public static boolean isAssignedTo(Interview interview, Long userId) {
        return userId != null && userId.equals(interview.getInterviewer().getId());
    }
}
