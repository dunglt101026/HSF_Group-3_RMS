package com.group3.rms.application.controller;

import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.application.service.CandidateApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/my/applications")
@RequiredArgsConstructor
public class MyApplicationController {

    private final CandidateApplicationService candidateApplicationService;

    // SCR-16: Hồ sơ ứng tuyển của tôi (UC-19).
    @GetMapping
    public String myApplications(@RequestParam(required = false) ApplicationStatus status, Model model) {
        model.addAttribute("list", candidateApplicationService.getMyApplications(status));
        model.addAttribute("statuses", ApplicationStatus.values());
        return "application/my-list";
    }
}
