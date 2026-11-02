package com.group3.rms.job.repository;

import com.group3.rms.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Truy vấn riêng (lọc, phân trang, @EntityGraph) do người phụ trách job/ bổ sung. */
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
}
