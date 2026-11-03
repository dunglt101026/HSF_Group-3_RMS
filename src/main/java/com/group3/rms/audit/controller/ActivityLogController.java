package com.group3.rms.audit.controller;

import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.service.ActivityLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/activity-logs")
public class ActivityLogController {

    private final ActivityLogService activityLogService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String listActivityLogs(
            @RequestParam(defaultValue = "") String action,
            @RequestParam(defaultValue = "") String actor,
            @RequestParam(defaultValue = "") String fromDate,
            @RequestParam(defaultValue = "") String toDate,
            @RequestParam(defaultValue = "0") String page,
            Model model
    ) {
        if (currentUserService.get().isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("action", action);
        model.addAttribute("actor", actor);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("auditActions", AuditAction.values());

        try {
            model.addAttribute("activityLogs", activityLogService.searchLogs(action, actor, fromDate, toDate, page));
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("invalidField", e.getField());
        }

        return "admin/activity-log";
    }
}
