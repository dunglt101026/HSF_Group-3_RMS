package com.group3.rms.audit.dto;

import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;

import java.time.LocalDateTime;

public record ActivityLogResponse(
        LocalDateTime createdAt,
        String actor,
        AuditAction action,
        TargetType targetType,
        Long targetId,
        String detail
) {}
