package com.group3.rms.audit.service;

import com.group3.rms.audit.dto.ActivityLogResponse;
import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.AuditLog;
import com.group3.rms.audit.mapper.ActivityLogMapper;
import com.group3.rms.audit.repository.AuditLogRepository;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.user.service.AdminUserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogServiceImpl implements ActivityLogService {

    private static final int PAGE_SIZE = 20;
    private static final int MAX_ACTOR_LENGTH = 50;

    private final AuditLogRepository auditLogRepository;
    private final ActivityLogMapper activityLogMapper;
    private final AdminUserService adminUserService;

    @Override
    public Page<ActivityLogResponse> searchLogs(String action, String actor, String fromDate, String toDate, String page) {
        adminUserService.checkAdminAccess();

        AuditAction actionFilter = parseAction(action);
        String actorPattern = createActorPattern(actor);
        LocalDate fromDateFilter = parseDate(fromDate, "fromDate");
        LocalDate toDateFilter = parseDate(toDate, "toDate");
        if (fromDateFilter != null && toDateFilter != null && fromDateFilter.isAfter(toDateFilter)) {
            throw new BusinessException("fromDate", "Từ ngày không được lớn hơn đến ngày.");
        }

        LocalDateTime startTime = fromDateFilter == null ? null : fromDateFilter.atStartOfDay();
        // DATETIME2 không có ngày sau 31/12/9999, nên ngày cuối này không cần cận trên.
        LocalDateTime endTime = toDateFilter == null || toDateFilter.equals(LocalDate.of(9999, 12, 31))
                ? null : toDateFilter.plusDays(1).atStartOfDay();

        Specification<AuditLog> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (actionFilter != null) {
                conditions.add(criteriaBuilder.equal(root.get("action"), actionFilter));
            }
            if (actorPattern != null) {
                conditions.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("actor")), actorPattern, '!'));
            }
            if (startTime != null) {
                conditions.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                conditions.add(criteriaBuilder.lessThan(root.get("createdAt"), endTime));
            }
            return criteriaBuilder.and(conditions.toArray(new Predicate[0]));
        };

        int pageNumber = parsePage(page);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, sort);
        Page<AuditLog> activityLogs = auditLogRepository.findAll(specification, pageable);
        if (pageNumber > 0 && pageNumber >= activityLogs.getTotalPages()) {
            throw new BusinessException("page", "Trang yêu cầu không tồn tại. Vui lòng quay về trang đầu.");
        }

        return activityLogs.map(activityLogMapper::toResponse);
    }

    private AuditAction parseAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }

        try {
            return AuditAction.valueOf(action);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("action", "Hành động lọc không hợp lệ. Vui lòng chọn lại hành động.");
        }
    }

    private String createActorPattern(String actor) {
        if (actor == null || actor.isBlank()) {
            return null;
        }

        String value = actor.trim();
        if (value.length() > MAX_ACTOR_LENGTH) {
            throw new BusinessException("actor", "Người thực hiện không được vượt quá 50 ký tự.");
        }

        // Escape cả dấu [ vì SQL Server dùng ký tự này để mở nhóm trong LIKE.
        value = value.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");
        return "%" + value + "%";
    }

    private LocalDate parseDate(String date, String field) {
        if (date == null || date.isBlank()) {
            return null;
        }

        String value = date.trim();
        String message = "Ngày lọc không hợp lệ. Vui lòng nhập theo định dạng yyyy-MM-dd, năm từ 0001 đến 9999.";
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new BusinessException(field, message);
        }

        try {
            LocalDate parsedDate = LocalDate.parse(value);
            if (parsedDate.getYear() < 1) {
                throw new BusinessException(field, message);
            }
            return parsedDate;
        } catch (DateTimeParseException e) {
            throw new BusinessException(field, message);
        }
    }

    private int parsePage(String page) {
        if (page == null || page.isBlank()) {
            return 0;
        }

        try {
            int pageNumber = Integer.parseInt(page.trim());
            // JPA nhận offset kiểu int, kiểm tra cả pageNumber * PAGE_SIZE.
            if (pageNumber < 0 || pageNumber > Integer.MAX_VALUE / PAGE_SIZE) {
                throw new BusinessException("page", "Số trang không hợp lệ. Vui lòng quay về trang đầu.");
            }
            return pageNumber;
        } catch (NumberFormatException e) {
            throw new BusinessException("page", "Số trang không hợp lệ. Vui lòng quay về trang đầu.");
        }
    }
}
