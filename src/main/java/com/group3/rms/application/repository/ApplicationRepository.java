package com.group3.rms.application.repository;

import com.group3.rms.application.dto.UpcomingInterviewResponse;
import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.interview.entity.InterviewStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    /** Đã nộp cho tin này chưa (kể cả đã rút) — UC-18. */
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);


    long countByJobId(Long jobId);

    /** UC-21: chi tiết hồ sơ kèm tin (phòng ban, HR phụ trách) và ứng viên trong một truy vấn. */
    @EntityGraph(attributePaths = {"job", "job.department", "job.owner", "candidate"})
    Optional<Application> findDetailById(Long id);

    /**
     * UC-22, UC-23: khóa dòng hồ sơ khi đổi trạng thái / thêm vòng phỏng vấn,
     * để hai thao tác đồng thời (HR bấm trùng, Candidate rút hồ sơ) không ghi đè nhau.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id")
    Optional<Application> findByIdForUpdate(@Param("id") Long id);

    /** UC-19: hồ sơ của một Candidate kèm tin, mới nộp trước. */
    @EntityGraph(attributePaths = "job")
    List<Application> findByCandidateIdOrderByCreatedAtDescIdDesc(Long candidateId);

    /**
     * UC-19: lịch phỏng vấn sắp tới của một Candidate, gần nhất trước.
     * Chỉ lấy buổi của hồ sơ đang phỏng vấn; không lấy tên người phỏng vấn và đánh giá.
     */
    @Query("""
            select new com.group3.rms.application.dto.UpcomingInterviewResponse(
                i.application.id, i.roundNo, i.scheduledAt, i.location)
            from Interview i
            where i.application.candidate.id = :candidateId
              and i.application.status = :applicationStatus
              and i.status = :interviewStatus
              and i.scheduledAt > :now
            order by i.scheduledAt asc
            """)
    List<UpcomingInterviewResponse> findUpcomingInterviews(
            @Param("candidateId") Long candidateId,
            @Param("applicationStatus") ApplicationStatus applicationStatus,
            @Param("interviewStatus") InterviewStatus interviewStatus,
            @Param("now") LocalDateTime now);
}
