package com.group3.rms.job.controller;

import com.group3.rms.job.service.PublicJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final PublicJobService publicJobService;

    // SCR-13: Việc làm (UC-16).
    @GetMapping
    public String listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute("jobs", publicJobService.searchJobs(keyword, departmentId, location, page));
        model.addAttribute("filters", publicJobService.getFilterOptions());
        model.addAttribute("keyword", keyword);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("location", location);
        return "career/list";
    }
}
