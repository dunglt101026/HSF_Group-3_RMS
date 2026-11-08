package com.group3.rms.interview.service;

import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.entity.InterviewStatus;

import java.time.LocalDateTime;


public final class InterviewRules {

    private InterviewRules() {
    }





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
