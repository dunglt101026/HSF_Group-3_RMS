package com.group3.rms.job.dto;

import java.util.List;


public record PublicJobFilterResponse(
        List<DepartmentOptionResponse> departments,
        List<String> locations
) {
}
