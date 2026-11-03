package com.group3.rms.user.controller;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") String page,
            Model model
    ) {
        if (currentUserService.get().isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("status", status);

        try {
            model.addAttribute("users", adminUserService.searchUsers(keyword, role, status, page));
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("invalidField", e.getField());
        }

        return "admin/user-list";
    }
}
