package com.group3.rms.interview.entity;

import com.group3.rms.application.entity.Application;
import com.group3.rms.common.entity.BaseEntity;
import com.group3.rms.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Length;

import java.time.LocalDateTime;

/** Một vòng phỏng vấn của hồ sơ, kèm đánh giá (rating, feedback) khi đã EVALUATED. */
@Entity
@Table(name = "interviews")
@Getter
@Setter
@NoArgsConstructor
public class Interview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    /** Người phỏng vấn: tài khoản HR hoặc Interviewer. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    /** Vòng thứ mấy: 1, 2, 3… */
    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    /** Phòng họp hoặc link họp online. */
    @Column(length = 500)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    /** 1–5, chỉ có khi đã đánh giá. */
    @Column(columnDefinition = "TINYINT")
    private Integer rating;

    @Column(length = Length.LONG32)
    private String feedback;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;
}
