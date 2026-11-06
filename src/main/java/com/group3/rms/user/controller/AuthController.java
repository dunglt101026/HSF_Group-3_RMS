package com.group3.rms.user.controller;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.RegisterRequest;
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
        // Đã đăng nhập thì về "/", HomeController chuyển tiếp theo vai trò
        if (currentUserService.get().isPresent()) {
            return "redirect:/";
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

        // Thông báo tài khoản bị khóa (UC-08)
        if (request.getParameter("locked") != null) {
            model.addAttribute("errorMessage", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        return "auth/login";
    }

    // ── Register ───────────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (currentUserService.get().isPresent()) {
            return "redirect:/";
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
}
