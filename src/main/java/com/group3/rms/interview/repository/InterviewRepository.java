package com.group3.rms.interview.repository;

import com.group3.rms.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    /** Các vòng của một hồ sơ, theo thứ tự vòng. */
    List<Interview> findByApplicationIdOrderByRoundNoAsc(Long applicationId);
}
