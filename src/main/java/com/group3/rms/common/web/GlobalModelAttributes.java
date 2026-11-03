package com.group3.rms.common.web;

import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final CurrentUserService currentUserService;

    @ModelAttribute("currentUser")
    public CurrentUser currentUser() {
        return currentUserService.get().orElse(null);
    }
}
