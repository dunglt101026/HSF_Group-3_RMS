package com.group3.rms.job.mapper;

import com.group3.rms.application.entity.Application;
import com.group3.rms.job.dto.JobApplicationRowResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobApplicationRowMapper {

    @Mapping(target = "candidateName", source = "application.candidate.fullName")
    @Mapping(target = "appliedAt", source = "application.createdAt")
    @Mapping(target = "interviewCount", source = "interviewCount")
    JobApplicationRowResponse toRow(Application application, long interviewCount);
}
