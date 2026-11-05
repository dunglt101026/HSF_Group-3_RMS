package com.group3.rms.job.mapper;

import com.group3.rms.job.dto.DepartmentOptionResponse;
import com.group3.rms.job.dto.PublicJobSummaryResponse;
import com.group3.rms.job.entity.Department;
import com.group3.rms.job.entity.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Mapper(componentModel = "spring")
public interface PublicJobMapper {

    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "salaryText", expression = "java(salaryText(job.getSalaryMin(), job.getSalaryMax()))")
    PublicJobSummaryResponse toSummary(Job job);

    DepartmentOptionResponse toDepartmentOption(Department department);

    /** Ví dụ: "15.000.000 – 30.000.000 VNĐ", "Từ 8.000.000 VNĐ", "Thỏa thuận". */
    default String salaryText(BigDecimal min, BigDecimal max) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
        if (min != null && max != null) {
            return format.format(min) + " – " + format.format(max) + " VNĐ";
        }
        if (min != null) {
            return "Từ " + format.format(min) + " VNĐ";
        }
        if (max != null) {
            return "Đến " + format.format(max) + " VNĐ";
        }
        return "Thỏa thuận";
    }
}
