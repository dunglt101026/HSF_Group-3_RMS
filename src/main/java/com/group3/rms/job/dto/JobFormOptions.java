package com.group3.rms.job.dto;

import com.group3.rms.job.entity.Department;
import com.group3.rms.job.entity.Skill;

import java.util.List;





public record JobFormOptions(
        List<Option> departments,
        List<Option> skills
) {

    public record Option(Long id, String name) {
    }

    public static JobFormOptions from(List<Department> departments, List<Skill> skills) {
        return new JobFormOptions(
                departments.stream()
                        .map(department -> new Option(department.getId(), department.getName()))
                        .toList(),
                skills.stream()
                        .map(skill -> new Option(skill.getId(), skill.getName()))
                        .toList()
        );
    }
}
