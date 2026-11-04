package com.group3.rms.application.controller;

import com.group3.rms.application.service.ApplicationReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationReviewController {

    private final ApplicationReviewService applicationReviewService;

    // SCR-17: Chi tiết hồ sơ (UC-21).
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("app", applicationReviewService.getDetail(id));
        return "application/detail";
    }
}
