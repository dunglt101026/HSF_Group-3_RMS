package com.group3.rms.job.mapper;

import com.group3.rms.job.dto.JobResponse;
import com.group3.rms.job.dto.JobSummaryResponse;
import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.Skill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.fullName")
    @Mapping(target = "skillIds", source = "skills")
    JobResponse toResponse(Job job);

    @Mapping(target = "id", source = "job.id")
    @Mapping(target = "title", source = "job.title")
    @Mapping(target = "departmentName", source = "job.department.name")
    @Mapping(target = "ownerId", source = "job.owner.id")
    @Mapping(target = "ownerName", source = "job.owner.fullName")
    @Mapping(target = "status", source = "job.status")
    @Mapping(target = "deadline", source = "job.deadline")
    @Mapping(target = "applicationCount", source = "applicationCount")
    JobSummaryResponse toSummary(Job job, long applicationCount);

    default String mapSkillName(Skill skill) {
        return skill.getName();
    }

    default Long mapSkillId(Skill skill) {
        return skill.getId();
    }
}
