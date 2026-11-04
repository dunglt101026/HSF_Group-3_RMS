
package com.group3.rms.job.mapper;

import com.group3.rms.job.dto.JobResponse;
import com.group3.rms.job.dto.JobSummaryResponse;
import com.group3.rms.job.entity.Job;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {


    public JobSummaryResponse toSummary(Job job, long applicationCount) {
        return new JobSummaryResponse(
                job.getId(),
                job.getTitle(),
                job.getDepartment().getName(),
                job.getOwner().getFullName(),
                job.getStatus(),
                job.getDeadline(),
                applicationCount
        );
    }



    public JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getLocation(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getDeadline(),
                job.getStatus(),
                job.getPublishedAt(),
                job.getDepartment().getName(),
                job.getOwner().getFullName(),
                job.getSkills().stream()
                        .map(skill -> skill.getName())
                        .toList(),
                job.getDepartment().getId(),
                job.getSkills().stream()
                        .map(skill -> skill.getId())
                        .toList()
        );
    }

}
