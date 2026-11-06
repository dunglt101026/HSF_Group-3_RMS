package com.group3.rms.application.mapper;

import com.group3.rms.application.dto.ApplyFormResponse;
import com.group3.rms.application.dto.MyApplicationResponse;
import com.group3.rms.application.dto.UpcomingInterviewResponse;
import com.group3.rms.application.entity.Application;
import com.group3.rms.job.entity.Job;
import com.group3.rms.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MyApplicationMapper {

    @Mapping(target = "id", source = "application.id")
    @Mapping(target = "jobId", source = "application.job.id")
    @Mapping(target = "jobTitle", source = "application.job.title")
    @Mapping(target = "appliedAt", source = "application.createdAt")
    @Mapping(target = "upcomingInterviews", source = "upcomingInterviews")
    MyApplicationResponse toResponse(Application application, List<UpcomingInterviewResponse> upcomingInterviews);

    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "jobTitle", source = "job.title")
    @Mapping(target = "candidateFullName", source = "candidate.fullName")
    @Mapping(target = "candidateEmail", source = "candidate.email")
    ApplyFormResponse toApplyForm(Job job, User candidate);
}
