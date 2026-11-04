package com.group3.rms.interview.mapper;

import com.group3.rms.interview.dto.MyInterviewResponse;
import com.group3.rms.interview.entity.Interview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InterviewMapper {

    @Mapping(target = "id", source = "interview.id")
    @Mapping(target = "applicationId", source = "interview.application.id")
    @Mapping(target = "candidateName", source = "interview.application.candidate.fullName")
    @Mapping(target = "jobTitle", source = "interview.application.job.title")
    @Mapping(target = "applicationStatus", source = "interview.application.status")
    @Mapping(target = "canEvaluate", source = "canEvaluate")
    MyInterviewResponse toMyInterview(Interview interview, boolean canEvaluate);
}
