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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public void log(AuditAction action, TargetType targetType, Long targetId, String detail) {
        String actor = currentUserService.get().map(CurrentUser::username).orElse("system");
        AuditLog auditLog = new AuditLog();
        auditLog.setActor(actor);
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setDetail(detail);

        auditLogRepository.save(auditLog);
        log.info("AuditLog: actor='{}', action='{}', targetType='{}', targetId='{}', detail='{}'",
                actor, action, targetType, targetId, detail);
    }
}
