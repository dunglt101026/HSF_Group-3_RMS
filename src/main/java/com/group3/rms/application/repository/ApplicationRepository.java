package com.group3.rms.application.repository;

import com.group3.rms.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    /** Đã nộp cho tin này chưa (kể cả đã rút) — UC-18. */
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);


    long countByJobId(Long jobId);

}
