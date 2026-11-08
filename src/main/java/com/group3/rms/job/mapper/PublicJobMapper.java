package com.group3.rms.job.mapper;

import com.group3.rms.job.dto.ApplyState;
import com.group3.rms.job.dto.DepartmentOptionResponse;
import com.group3.rms.job.dto.PublicJobDetailResponse;
import com.group3.rms.job.dto.PublicJobSummaryResponse;
import com.group3.rms.job.entity.Department;
import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.Skill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface PublicJobMapper {

    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "salaryText", expression = "java(salaryText(job.getSalaryMin(), job.getSalaryMax()))")
    PublicJobSummaryResponse toSummary(Job job);

    @Mapping(target = "id", source = "job.id")
    @Mapping(target = "departmentName", source = "job.department.name")
    @Mapping(target = "salaryText", expression = "java(salaryText(job.getSalaryMin(), job.getSalaryMax()))")
    @Mapping(target = "skills", source = "job.skills")
    @Mapping(target = "applyState", source = "applyState")
    PublicJobDetailResponse toDetail(Job job, ApplyState applyState);

    DepartmentOptionResponse toDepartmentOption(Department department);


    default List<String> skillNames(Set<Skill> skills) {
        return skills.stream().map(Skill::getName).sorted().toList();
    }


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
