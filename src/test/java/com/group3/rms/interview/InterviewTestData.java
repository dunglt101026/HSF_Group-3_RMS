package com.group3.rms.interview;

import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.entity.InterviewStatus;
import com.group3.rms.job.entity.Department;
import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

/** Dựng nhanh entity cho unit test của chức năng xử lý hồ sơ & phỏng vấn (không cần DB). */
public final class InterviewTestData {

    public static final long HR_OWNER_ID = 2L;
    public static final long HR_OTHER_ID = 3L;
    public static final long INTERVIEWER_ID = 4L;
    public static final long INTERVIEWER_OTHER_ID = 5L;
    public static final long CANDIDATE_ID = 6L;

    private InterviewTestData() {
    }

    public static CurrentUser currentUser(long id, RoleCode role) {
        return new CurrentUser(id, "user" + id, "Người dùng " + id, role);
    }

    public static User user(long id, String fullName) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername("user" + id);
        user.setEmail("user" + id + "@rms.local");
        user.setFullName(fullName);
        user.setPhone("0900000000");
        return user;
    }

    public static Application application(long id, ApplicationStatus status) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", 1L);
        department.setName("Phòng Kỹ thuật");

        Job job = new Job();
        ReflectionTestUtils.setField(job, "id", 10L);
        job.setTitle("Lập trình viên Java");
        job.setStatus(JobStatus.ACTIVE);
        job.setDepartment(department);
        job.setOwner(user(HR_OWNER_ID, "HR Phụ Trách"));

        Application application = new Application();
        ReflectionTestUtils.setField(application, "id", id);
        ReflectionTestUtils.setField(application, "createdAt", LocalDateTime.now().minusDays(3));
        application.setJob(job);
        application.setCandidate(user(CANDIDATE_ID, "Nguyễn Văn An"));
        application.setCvPath("seed/cv.pdf");
        application.setCvOriginalName("NguyenVanAn_CV.pdf");
        application.setCoverLetter("Thư giới thiệu");
        application.setStatus(status);
        return application;
    }

    public static Interview interview(long id, Application application, long interviewerId, int roundNo,
                                      LocalDateTime scheduledAt, InterviewStatus status) {
        Interview interview = new Interview();
        ReflectionTestUtils.setField(interview, "id", id);
        interview.setApplication(application);
        interview.setInterviewer(user(interviewerId, "Người phỏng vấn " + interviewerId));
        interview.setRoundNo(roundNo);
        interview.setScheduledAt(scheduledAt);
        interview.setLocation("Phòng họp 301");
        interview.setStatus(status);
        if (status == InterviewStatus.EVALUATED) {
            interview.setRating(4);
            interview.setFeedback("Ứng viên tốt");
            interview.setEvaluatedAt(scheduledAt.plusHours(1));
        }
        return interview;
    }
}
