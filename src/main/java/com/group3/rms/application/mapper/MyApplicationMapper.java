package com.group3.rms.application.mapper;

import com.group3.rms.application.dto.ApplyFormResponse;
import com.group3.rms.job.entity.Job;
import com.group3.rms.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface MyApplicationMapper {

    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "jobTitle", source = "job.title")
    @Mapping(target = "candidateFullName", source = "candidate.fullName")
    @Mapping(target = "candidateEmail", source = "candidate.email")
    ApplyFormResponse toApplyForm(Job job, User candidate);
}
