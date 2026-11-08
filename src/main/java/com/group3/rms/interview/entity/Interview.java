package com.group3.rms.interview.entity;

import com.group3.rms.application.entity.Application;
import com.group3.rms.common.entity.BaseEntity;
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
import org.hibernate.Length;

import java.time.LocalDateTime;


@Entity
@Table(name = "interviews",

        uniqueConstraints = @UniqueConstraint(name = "uq_interviews_application_round",
                columnNames = {"application_id", "round_no"}),
        indexes = @Index(name = "ix_interviews_interviewer_status_time",
                columnList = "interviewer_id, status, scheduled_at"))
@Getter
@Setter
@NoArgsConstructor
public class Interview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;


    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;


    @Column(length = 500)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status = InterviewStatus.SCHEDULED;


    @Column(columnDefinition = "TINYINT")
    private Integer rating;

    @Column(length = Length.LONG32)
    private String feedback;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;
}
