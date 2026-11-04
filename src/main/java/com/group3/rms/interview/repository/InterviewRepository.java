package com.group3.rms.interview.repository;

import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.entity.InterviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    /** Các vòng của một hồ sơ, theo thứ tự vòng. */
    List<Interview> findByApplicationIdOrderByRoundNoAsc(Long applicationId);

    // ===== UC-24: lịch phỏng vấn của tôi — tải sẵn hồ sơ, tin, ứng viên để hiển thị bảng =====

    /** Tab "Sắp tới": chưa tới giờ, gần nhất trước. */
    @EntityGraph(attributePaths = {"application", "application.job", "application.candidate"})
    List<Interview> findByInterviewerIdAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
            Long interviewerId, InterviewStatus status, LocalDateTime now);

    /** Tab "Cần đánh giá": đã tới giờ nhưng chưa đánh giá, buổi cũ nhất trước. */
    @EntityGraph(attributePaths = {"application", "application.job", "application.candidate"})
    List<Interview> findByInterviewerIdAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            Long interviewerId, InterviewStatus status, LocalDateTime now);

    /** Tab "Đã đánh giá": buổi mới nhất trước. */
    @EntityGraph(attributePaths = {"application", "application.job", "application.candidate"})
    List<Interview> findByInterviewerIdAndStatusOrderByScheduledAtDesc(Long interviewerId, InterviewStatus status);

    long countByInterviewerIdAndStatusAndScheduledAtAfter(Long interviewerId, InterviewStatus status, LocalDateTime now);

    long countByInterviewerIdAndStatusAndScheduledAtLessThanEqual(
            Long interviewerId, InterviewStatus status, LocalDateTime now);

    long countByInterviewerIdAndStatus(Long interviewerId, InterviewStatus status);
}
