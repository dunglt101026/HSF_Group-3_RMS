package com.group3.rms.application.repository;

import com.group3.rms.application.entity.Application;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    /** Đã nộp cho tin này chưa (kể cả đã rút) — UC-18. */
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);


    long countByJobId(Long jobId);

    /** UC-21: chi tiết hồ sơ kèm tin (phòng ban, HR phụ trách) và ứng viên trong một truy vấn. */
    @EntityGraph(attributePaths = {"job", "job.department", "job.owner", "candidate"})
    Optional<Application> findDetailById(Long id);
}
