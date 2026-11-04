
package com.group3.rms.job.repository;

import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>,
        JpaSpecificationExecutor<Job> {

    // Lấy danh sách tin có phân trang.
    // Tải sẵn phòng ban và HR phụ trách để tránh truy vấn lặp.
    @Override
    @EntityGraph(attributePaths = {"department", "owner"})
    Page<Job> findAll(
            Specification<Job> specification,
            Pageable pageable
    );

    // Lấy chi tiết tin, bao gồm phòng ban, HR phụ trách và kỹ năng.
    @EntityGraph(attributePaths = {
            "department",
            "owner",
            "skills"
    })
    Optional<Job> findByIdAndDeletedFalse(Long id);

    List<Job> findAllByStatusAndDeadlineLessThanAndDeletedFalse(
            JobStatus status,
            LocalDate deadline
    );
}
