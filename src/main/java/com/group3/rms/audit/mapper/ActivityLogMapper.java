package com.group3.rms.audit.mapper;

import com.group3.rms.audit.dto.ActivityLogResponse;
import com.group3.rms.audit.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ActivityLogMapper {

    ActivityLogResponse toResponse(AuditLog auditLog);
}
