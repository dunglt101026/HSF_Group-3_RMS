package com.group3.rms.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record InterviewEvaluationRequest(

        @NotNull(message = "Vui lòng chọn điểm đánh giá.")
        @Min(value = 1, message = "Điểm từ 1 đến 5.")
        @Max(value = 5, message = "Điểm từ 1 đến 5.")
        Integer rating,

        @NotBlank(message = "Vui lòng nhập nhận xét.")
        @Size(max = 4000, message = "Nhận xét tối đa 4000 ký tự.")
        String feedback
) {

    public static InterviewEvaluationRequest empty() {
        return new InterviewEvaluationRequest(null, null);
    }
}
