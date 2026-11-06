package com.group3.rms.application.repository;

import com.group3.rms.application.entity.Application;
import com.group3.rms.application.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    /** Đã nộp cho tin này chưa (kể cả đã rút) — UC-18. */
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    long countByJobId(Long jobId);

    /**
     * Danh sách hồ sơ kèm ứng viên trong một truy vấn — SCR-12 (UC-15).
     * Bổ sung read-only theo quyết định D1: cần TV4/TV5 xác nhận trước khi merge.
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
}
