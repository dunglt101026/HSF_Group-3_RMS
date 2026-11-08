package com.group3.rms.interview.repository;

import com.group3.rms.interview.dto.InterviewerOptionResponse;
import com.group3.rms.interview.entity.Interview;
import com.group3.rms.interview.entity.InterviewStatus;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {


    List<Interview> findByApplicationIdOrderByRoundNoAsc(Long applicationId);





    @Query("""
            SELECT i.application.id AS applicationId, COUNT(i) AS total
            FROM Interview i
            WHERE i.application.id IN :applicationIds
            GROUP BY i.application.id
            """)
    List<ApplicationInterviewCount> countGroupedByApplicationIds(
            @Param("applicationIds") Collection<Long> applicationIds
    );

    interface ApplicationInterviewCount {
        Long getApplicationId();

        long getTotal();
    }


    @EntityGraph(attributePaths = "interviewer")
    List<Interview> findWithInterviewerByApplicationIdOrderByRoundNoAsc(Long applicationId);




    @EntityGraph(attributePaths = {"application", "application.job", "application.candidate"})
    List<Interview> findByInterviewerIdAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
            Long interviewerId, InterviewStatus status, LocalDateTime now);


    @EntityGraph(attributePaths = {"application", "application.job", "application.candidate"})
    List<Interview> findByInterviewerIdAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            Long interviewerId, InterviewStatus status, LocalDateTime now);


    @EntityGraph(attributePaths = {"application", "application.job", "application.candidate"})
    List<Interview> findByInterviewerIdAndStatusOrderByScheduledAtDesc(Long interviewerId, InterviewStatus status);

    long countByInterviewerIdAndStatusAndScheduledAtAfter(Long interviewerId, InterviewStatus status, LocalDateTime now);

    long countByInterviewerIdAndStatusAndScheduledAtLessThanEqual(
            Long interviewerId, InterviewStatus status, LocalDateTime now);

    long countByInterviewerIdAndStatus(Long interviewerId, InterviewStatus status);




    long countByApplicationId(Long applicationId);





    @Query("""
            select new com.group3.rms.interview.dto.InterviewerOptionResponse(u.id, u.fullName, r.code)
            from User u join u.roles r
            where u.status = :status and r.code in :roles
            order by u.fullName asc
            """)
    List<InterviewerOptionResponse> findAssignableInterviewers(@Param("status") UserStatus status,
                                                               @Param("roles") Collection<RoleCode> roles);


    @Query("""
            select u from User u join u.roles r
            where u.id = :id and u.status = :status and r.code in :roles
            """)
    Optional<User> findAssignableInterviewer(@Param("id") Long id,
                                             @Param("status") UserStatus status,
                                             @Param("roles") Collection<RoleCode> roles);




    @EntityGraph(attributePaths = {"application", "application.job", "application.candidate", "interviewer"})
    Optional<Interview> findForEvaluationById(Long id);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Interview i where i.id = :id")
    Optional<Interview> findByIdForUpdate(@Param("id") Long id);
}
