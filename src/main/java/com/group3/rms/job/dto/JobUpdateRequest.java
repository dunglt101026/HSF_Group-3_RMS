package com.group3.rms.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record JobUpdateRequest(

        @NotBlank(message = "Tiêu đề tin tuyển dụng không được để trống.")
        @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự.")
        String title,

        @NotBlank(message = "Mô tả công việc không được để trống.")
        String description,

        @Size(max = 200, message = "Địa điểm làm việc không được vượt quá 200 ký tự.")
        String location,

        @PositiveOrZero(message = "Lương tối thiểu không được âm.")
        BigDecimal salaryMin,

        @PositiveOrZero(message = "Lương tối đa không được âm.")
        BigDecimal salaryMax,

        @NotNull(message = "Hạn nộp hồ sơ không được để trống.")
        LocalDate deadline,

        @NotNull(message = "Vui lòng chọn phòng ban.")
        Long departmentId,

        @NotEmpty(message = "Vui lòng chọn ít nhất một kỹ năng.")
        Set<Long> skillIds
) {
}
