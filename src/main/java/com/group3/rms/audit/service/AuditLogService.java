package com.group3.rms.audit.service;

import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;

public interface AuditLogService {

    void log(AuditAction action, TargetType targetType, Long targetId, String detail);

    /** Ghi nhật ký với tên đăng nhập đã xác định cho thao tác chưa có phiên đăng nhập. */
    void log(String actor, AuditAction action, TargetType targetType, Long targetId, String detail);
}
