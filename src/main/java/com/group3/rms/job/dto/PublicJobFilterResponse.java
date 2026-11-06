package com.group3.rms.job.dto;

import java.util.List;

/** Các lựa chọn của bộ lọc ở SCR-13. */
public record PublicJobFilterResponse(
        List<DepartmentOptionResponse> departments,
        List<String> locations
) {
}
