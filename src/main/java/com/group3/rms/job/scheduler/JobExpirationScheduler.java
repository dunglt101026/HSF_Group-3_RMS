package com.group3.rms.job.scheduler;

import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobExpirationScheduler {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final JobRepository jobRepository;
    private final AuditLogService auditLogService;

    @Scheduled(
            cron = "0 5 0 * * *",
            zone = "Asia/Ho_Chi_Minh"
    )
    @Transactional
    public void closeExpiredJobs() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        List<Job> expiredJobs =
                jobRepository.findAllByStatusAndDeadlineLessThanAndDeletedFalse(
                        JobStatus.ACTIVE,
                        today
                );

        if (expiredJobs.isEmpty()) {
            log.info("No expired jobs found.");
            return;
        }

        for (Job job : expiredJobs) {
            job.setStatus(JobStatus.CLOSED);

            auditLogService.log(
                    AuditAction.CLOSE_JOB,
                    TargetType.JOB,
                    job.getId(),
                    "Hệ thống tự động đóng tin tuyển dụng quá hạn: "
                            + job.getTitle()
            );
        }

        jobRepository.saveAll(expiredJobs);

        log.info("Automatically closed {} expired jobs.",
                expiredJobs.size());
    }
}
