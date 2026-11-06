package com.group3.rms.interview.mapper;

import com.group3.rms.interview.dto.InterviewEvaluationResponse;
import com.group3.rms.interview.dto.InterviewRoundResponse;
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

    /** Ẩn điểm và nhận xét khi người xem không được thấy đánh giá của vòng này (UC-21). */
    @Mapping(target = "id", source = "interview.id")
    @Mapping(target = "interviewerName", source = "interview.interviewer.fullName")
    @Mapping(target = "rating", expression = "java(evaluationVisible ? interview.getRating() : null)")
    @Mapping(target = "feedback", expression = "java(evaluationVisible ? interview.getFeedback() : null)")
    @Mapping(target = "evaluationVisible", source = "evaluationVisible")
    @Mapping(target = "canEvaluate", source = "canEvaluate")
    InterviewRoundResponse toRound(Interview interview, boolean evaluationVisible, boolean canEvaluate);

    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "candidateName", source = "application.candidate.fullName")
    @Mapping(target = "jobTitle", source = "application.job.title")
    InterviewEvaluationResponse toEvaluation(Interview interview);
}
