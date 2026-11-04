package com.group3.rms.application.mapper;

import com.group3.rms.application.dto.ApplicationActions;
import com.group3.rms.application.dto.ApplicationDetailResponse;
import com.group3.rms.application.entity.Application;
import com.group3.rms.interview.dto.InterviewRoundResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "id", source = "application.id")
    @Mapping(target = "jobId", source = "application.job.id")
    @Mapping(target = "jobTitle", source = "application.job.title")
    @Mapping(target = "departmentName", source = "application.job.department.name")
    @Mapping(target = "candidateFullName", source = "application.candidate.fullName")
    @Mapping(target = "candidateEmail", source = "application.candidate.email")
    @Mapping(target = "candidatePhone", source = "application.candidate.phone")
    @Mapping(target = "appliedAt", source = "application.createdAt")
    @Mapping(target = "interviews", source = "interviews")
    @Mapping(target = "actions", source = "actions")
    ApplicationDetailResponse toDetail(Application application,
                                       List<InterviewRoundResponse> interviews,
                                       ApplicationActions actions);
}
