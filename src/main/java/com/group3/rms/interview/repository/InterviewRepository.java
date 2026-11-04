package com.group3.rms.interview.repository;

import com.group3.rms.interview.dto.InterviewerOptionResponse;
import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.entity.InterviewStatus;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    /** Các vòng của một hồ sơ, theo thứ tự vòng. */
    List<Interview> findByApplicationIdOrderByRoundNoAsc(Long applicationId);

    /** UC-21: các vòng của hồ sơ kèm người phỏng vấn, để hiện bảng ở SCR-17. */
    @EntityGraph(attributePaths = "interviewer")
    List<Interview> findWithInterviewerByApplicationIdOrderByRoundNoAsc(Long applicationId);

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

    // ===== UC-23: lên lịch phỏng vấn =====

    /** Số vòng hiện có của hồ sơ; vòng mới = số này + 1. */
    long countByApplicationId(Long applicationId);

    /**
     * Danh sách người có thể được giao phỏng vấn: tài khoản đang hoạt động, vai trò HR hoặc Interviewer,
     * sắp theo tên. Chỉ đọc bảng users (không ghi), nên đặt ở đây để không sửa repository của chức năng user.
     */
    @Query("""
            select new com.group3.rms.interview.dto.InterviewerOptionResponse(u.id, u.fullName, r.code)
            from User u join u.roles r
            where u.status = :status and r.code in :roles
            order by u.fullName asc
            """)
    List<InterviewerOptionResponse> findAssignableInterviewers(@Param("status") UserStatus status,
                                                               @Param("roles") Collection<RoleCode> roles);

    /** Kiểm tra lại người phỏng vấn được chọn khi lưu (có thể vừa bị khóa / đổi vai trò). */
    @Query("""
            select u from User u join u.roles r
            where u.id = :id and u.status = :status and r.code in :roles
            """)
    Optional<User> findAssignableInterviewer(@Param("id") Long id,
                                             @Param("status") UserStatus status,
                                             @Param("roles") Collection<RoleCode> roles);
}
