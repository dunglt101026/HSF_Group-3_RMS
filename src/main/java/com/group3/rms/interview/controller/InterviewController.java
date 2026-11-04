package com.group3.rms.interview.controller;

import com.group3.rms.interview.dto.MyInterviewTab;
import com.group3.rms.interview.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    // SCR-19: Lịch phỏng vấn của tôi (UC-24).
    @GetMapping("/interviews/my")
    public String mySchedule(
            @RequestParam(defaultValue = "UPCOMING") MyInterviewTab tab,
            Model model
    ) {
        model.addAttribute("schedule", interviewService.getMySchedule(tab));
        return "interview/my-schedule";
    }
}
