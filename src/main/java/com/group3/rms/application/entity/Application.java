package com.group3.rms.application.entity;

import com.group3.rms.common.entity.BaseEntity;
import com.group3.rms.job.entity.Job;
import com.group3.rms.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Hồ sơ ứng tuyển. Mỗi Candidate nộp 1 lần cho mỗi tin (UNIQUE job_id + candidate_id). */
@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(name = "uq_applications_job_candidate",
                columnNames = {"job_id", "candidate_id"}),
        indexes = {
                // Đếm / lọc hồ sơ theo tin và trạng thái (SCR-09, SCR-11, SCR-12).
                @Index(name = "ix_applications_job_status", columnList = "job_id, status"),
                // Danh sách hồ sơ của một ứng viên (SCR-16).
                @Index(name = "ix_applications_candidate", columnList = "candidate_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Application extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    /** Đường dẫn file CV đã đặt lại tên, lưu ngoài thư mục web. */
    @Column(name = "cv_path", nullable = false, length = 500)
    private String cvPath;

    /** Tên file gốc, dùng khi tải về. */
    @Column(name = "cv_original_name", nullable = false, length = 255)
    private String cvOriginalName;

    @Column(name = "cover_letter", length = 2000)
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;
}
