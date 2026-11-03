package com.group3.rms.user.controller;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.PasswordChangeRequest;
import com.group3.rms.user.dto.ProfileUpdateRequest;
import com.group3.rms.user.dto.UserResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String viewProfile(Model model) {
        Optional<CurrentUser> currentOpt = currentUserService.get();
        if (currentOpt.isEmpty()) {
            return "redirect:/login";
        }

        UserResponse user = userService.getCurrentUserProfile();
        model.addAttribute("user", user);

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ProfileUpdateRequest(user.fullName(), user.phone()));
        }
        return "profile/view";
    }

    @PostMapping
    public String updateProfile(
            @Valid @ModelAttribute("form") ProfileUpdateRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        Optional<CurrentUser> currentOpt = currentUserService.get();
        if (currentOpt.isEmpty()) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getCurrentUserProfile());
            return "profile/view";
        }

        try {
            userService.updateProfile(form);
            ra.addFlashAttribute("success", "Cập nhật thông tin thành công.");
            return "redirect:/profile";
        } catch (BusinessException e) {
            e.applyTo(bindingResult);
            model.addAttribute("user", userService.getCurrentUserProfile());
            return "profile/view";
        }
    }

    @GetMapping("/password")
    public String passwordPage(Model model) {
        Optional<CurrentUser> currentOpt = currentUserService.get();
        if (currentOpt.isEmpty()) {
            return "redirect:/login";
        }

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new PasswordChangeRequest("", "", ""));
        }
        return "profile/password";
    }

    @PostMapping("/password")
    public String changePassword(
            @Valid @ModelAttribute("form") PasswordChangeRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        Optional<CurrentUser> currentOpt = currentUserService.get();
        if (currentOpt.isEmpty()) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            return "profile/password";
        }

        try {
            userService.changePassword(form);
            ra.addFlashAttribute("success", "Đổi mật khẩu thành công.");
            return "redirect:/profile";
        } catch (BusinessException e) {
            e.applyTo(bindingResult);
            return "profile/password";
        }
    }
}
