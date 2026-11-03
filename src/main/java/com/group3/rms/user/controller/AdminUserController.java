package com.group3.rms.user.controller;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.AdminUserCreateRequest;
import com.group3.rms.user.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentUserService currentUserService;

    @InitBinder("form")
    public void initCreateForm(WebDataBinder binder) {
        binder.setAllowedFields("fullName", "username", "email", "phone", "role", "password");
    }

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

    @GetMapping("/new")
    public String createUserPage(Model model) {
        if (currentUserService.get().isEmpty()) {
            return "redirect:/login";
        }

        adminUserService.checkCreatePermission();
        model.addAttribute("form", new AdminUserCreateRequest("", "", "", "", "", ""));
        return "admin/user-form";
    }

    @PostMapping("/new")
    public String createUser(
            @Valid @ModelAttribute("form") AdminUserCreateRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        bindingResult = clearPassword(form, bindingResult, model);

        if (currentUserService.get().isEmpty()) {
            return "redirect:/login";
        }

        // Kiểm tra quyền cả khi form sai, tránh trả form cho người không có quyền.
        adminUserService.checkCreatePermission();
        if (!bindingResult.hasErrors()) {
            try {
                try {
                    adminUserService.createUser(form);
                } catch (DataIntegrityViolationException e) {
                    // Giao dịch tạo đã rollback; truy vấn lại trong giao dịch đọc mới.
                    adminUserService.checkAccountAvailability(form.username(), form.email());
                    throw e;
                }

                ra.addFlashAttribute("success", "Đã tạo tài khoản.");
                return "redirect:/admin/users";
            } catch (BusinessException e) {
                e.applyTo(bindingResult);
            }
        }

        return "admin/user-form";
    }

    private BindingResult clearPassword(AdminUserCreateRequest form, BindingResult bindingResult, Model model) {
        AdminUserCreateRequest safeForm = new AdminUserCreateRequest(
                form.fullName(), form.username(), form.email(), form.phone(), form.role(), null
        );
        BindingResult safeResult = new BeanPropertyBindingResult(safeForm, "form");

        // Xóa cả mật khẩu trong DTO lẫn rejectedValue của lỗi validation.
        for (ObjectError error : bindingResult.getAllErrors()) {
            if (error instanceof FieldError fieldError) {
                Object rejectedValue = fieldError.getRejectedValue();
                if ("password".equals(fieldError.getField())) {
                    rejectedValue = null;
                }
                safeResult.addError(new FieldError(
                        "form", fieldError.getField(), rejectedValue, fieldError.isBindingFailure(),
                        fieldError.getCodes(), fieldError.getArguments(), fieldError.getDefaultMessage()
                ));
            } else {
                safeResult.addError(new ObjectError(
                        "form", error.getCodes(), error.getArguments(), error.getDefaultMessage()
                ));
            }
        }

        model.addAttribute("form", safeForm);
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "form", safeResult);
        return safeResult;
    }
}
