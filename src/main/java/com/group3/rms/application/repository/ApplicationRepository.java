package com.group3.rms.application.repository;

import com.group3.rms.application.dto.UpcomingInterviewResponse;
import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.interview.entity.InterviewStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    /** Đã nộp cho tin này chưa (kể cả đã rút) — UC-18. */
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    long countByJobId(Long jobId);

    /**
     * Danh sách hồ sơ kèm ứng viên trong một truy vấn — SCR-12 (UC-15).
     * Bổ sung read-only theo quyết định D1.
     */
    @Override
    @EntityGraph(attributePaths = {"candidate"})
    Page<Application> findAll(Specification<Application> specification, Pageable pageable);

    /** Đếm hồ sơ của một tin theo từng trạng thái — SCR-11, SCR-12. */
    @Query("""
            SELECT a.status AS status, COUNT(a) AS total
            FROM Application a
            WHERE a.job.id = :jobId
            GROUP BY a.status
            """)
    List<StatusCount> countGroupedByStatus(@Param("jobId") Long jobId);

    /** Đếm hồ sơ của nhiều tin trong một truy vấn — tránh N+1 ở SCR-09. */
    @Query("""
            SELECT a.job.id AS jobId, COUNT(a) AS total
            FROM Application a
            WHERE a.job.id IN :jobIds
            GROUP BY a.job.id
            """)
    List<JobCount> countGroupedByJobIds(@Param("jobIds") Collection<Long> jobIds);

    interface StatusCount {
        ApplicationStatus getStatus();

        long getTotal();
    }

    interface JobCount {
        Long getJobId();

        long getTotal();
    }

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
