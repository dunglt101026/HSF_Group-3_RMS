package com.group3.rms.user.controller;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.RegisterRequest;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    // ── Login (GET only — POST xử lý bởi Spring Security) ─────────────────

    @GetMapping("/login")
    public String loginPage(
            HttpServletRequest request,
            Model model
    ) {
        // Nếu đã đăng nhập thì redirect về trang chính theo role
        Optional<CurrentUser> currentOpt = currentUserService.get();
        if (currentOpt.isPresent()) {
            return "redirect:" + redirectUrlForRole(currentOpt.get().role());
        }

        // Lấy thông báo lỗi từ Spring Security failure handler
        HttpSession session = request.getSession(false);
        if (session != null) {
            String loginError = (String) session.getAttribute("loginError");
            if (loginError != null) {
                model.addAttribute("errorMessage", loginError);
                session.removeAttribute("loginError");
            }
        }

        // Thông báo đăng xuất thành công
        if (request.getParameter("logout") != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất thành công.");
        }

        return "auth/login";
    }

    // ── Register ───────────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage(Model model) {
        Optional<CurrentUser> currentOpt = currentUserService.get();
        if (currentOpt.isPresent()) {
            return "redirect:" + redirectUrlForRole(currentOpt.get().role());
        }

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterRequest("", "", "", "", ""));
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegisterRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(form);
            ra.addFlashAttribute("success", "Tạo tài khoản thành công, vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (BusinessException e) {
            e.applyTo(bindingResult);
            return "auth/register";
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String redirectUrlForRole(RoleCode role) {
        if (role == null) return "/";
        return switch (role) {
            case ADMIN       -> "/admin/users";
            case HR          -> "/hr/jobs";
            case INTERVIEWER -> "/interviews/my";
            case CANDIDATE   -> "/";
        };
    }
}
