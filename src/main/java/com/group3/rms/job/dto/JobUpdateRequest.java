
package com.group3.rms.job.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record JobUpdateRequest(
        String title,
        String description,
        String location,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        LocalDate deadline,
        Long departmentId,
        Set<Long> skillIds
) {
}
