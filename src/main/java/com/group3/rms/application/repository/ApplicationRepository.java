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


    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    long countByJobId(Long jobId);





    @Override
    @EntityGraph(attributePaths = {"candidate"})
    Page<Application> findAll(Specification<Application> specification, Pageable pageable);


    @Query("""
            SELECT a.status AS status, COUNT(a) AS total
            FROM Application a
            WHERE a.job.id = :jobId
            GROUP BY a.status
            """)
    List<StatusCount> countGroupedByStatus(@Param("jobId") Long jobId);


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


    @EntityGraph(attributePaths = {"job", "job.department", "job.owner", "candidate"})
    Optional<Application> findDetailById(Long id);





    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id")
    Optional<Application> findByIdForUpdate(@Param("id") Long id);


    @EntityGraph(attributePaths = "job")
    List<Application> findByCandidateIdOrderByCreatedAtDescIdDesc(Long candidateId);





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
