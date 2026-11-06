package com.group3.rms.interview.repository;

import com.group3.rms.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    /** Các vòng của một hồ sơ, theo thứ tự vòng. */
    List<Interview> findByApplicationIdOrderByRoundNoAsc(Long applicationId);

    /**
     * Đếm số vòng phỏng vấn của nhiều hồ sơ trong một truy vấn — SCR-12 (UC-15).
     * Bổ sung read-only theo quyết định D1: cần TV5 xác nhận trước khi merge.
     */
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
}
