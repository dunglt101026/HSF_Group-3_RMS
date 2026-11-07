package com.group3.rms.audit.service;

import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.AuditLog;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.repository.AuditLogRepository;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public void log(AuditAction action, TargetType targetType, Long targetId, String detail) {
        validateAction(action);

        Optional<CurrentUser> currentUser = currentUserService.get();
        String actor = currentUser.map(CurrentUser::username).orElse(null);
        // Chỉ dùng system khi chưa đăng nhập, không thay thế username không hợp lệ.
        if (currentUser.isEmpty()) {
            actor = "system";
        }
        saveLog(actor, action, targetType, targetId, detail);
    }

    @Override
    @Transactional
    public void log(String actor, AuditAction action, TargetType targetType, Long targetId, String detail) {
        validateAction(action);
        saveLog(actor, action, targetType, targetId, detail);
    }

    private void validateAction(AuditAction action) {
        if (action == null) {
            throw new IllegalArgumentException("Hành động ghi nhật ký không được để trống.");
        }
    }

    private void saveLog(String actor, AuditAction action, TargetType targetType, Long targetId, String detail) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("Người thực hiện nhật ký không được để trống.");
        }
        if (actor.length() > 50) {
            throw new IllegalArgumentException("Tên đăng nhập người thực hiện nhật ký không được vượt quá 50 ký tự.");
        }
        if (targetId != null && targetId <= 0) {
            throw new IllegalArgumentException("ID đối tượng ghi nhật ký phải lớn hơn 0.");
        }
        if (detail != null && detail.length() > 500) {
            throw new IllegalArgumentException("Chi tiết nhật ký không được vượt quá 500 ký tự.");
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setActor(actor);
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setDetail(detail);

        auditLogRepository.save(auditLog);
        log.info("AuditLog: action='{}', targetType='{}', targetId='{}'", action, targetType, targetId);
    }
}
