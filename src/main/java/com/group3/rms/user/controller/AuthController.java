package com.group3.rms.user.controller;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.LoginRequest;
import com.group3.rms.user.dto.UserResponse;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.service.UserService;
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

    @GetMapping("/login")
    public String loginPage(Model model) {
        Optional<CurrentUser> currentOpt = currentUserService.get();
        if (currentOpt.isPresent()) {
            return "redirect:" + redirectUrlForRole(currentOpt.get().role());
        }

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new LoginRequest("", ""));
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("form") LoginRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            UserResponse user = userService.login(form);
            ra.addFlashAttribute("success", "Đăng nhập thành công. Xin chào " + user.fullName() + "!");
            return "redirect:" + redirectUrlForRole(user.role());
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/login";
        }
    }

    private String redirectUrlForRole(RoleCode role) {
        if (role == null) {
            return "/";
        }
        return switch (role) {
            case ADMIN -> "/admin/users";
            case HR -> "/hr/jobs";
            case INTERVIEWER -> "/interviews/my";
            case CANDIDATE -> "/";
        };
    }
}
