package com.group3.rms.job.controller;

import com.group3.rms.job.service.PublicJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final PublicJobService publicJobService;


    @GetMapping
    public String listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String page,
            Model model
    ) {

        Long department = parseNumber(departmentId);
        Long pageNumber = parseNumber(page);
        int safePage = pageNumber == null ? 0 : Math.clamp(pageNumber, 0, Integer.MAX_VALUE);

        model.addAttribute("jobs", publicJobService.searchJobs(keyword, department, location, safePage));
        model.addAttribute("filters", publicJobService.getFilterOptions());
        model.addAttribute("keyword", keyword);
        model.addAttribute("departmentId", department);
        model.addAttribute("location", location);
        return "career/list";
    }

    private Long parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }


    @GetMapping("/{id}")
    public String jobDetail(@PathVariable Long id, Model model) {
        model.addAttribute("job", publicJobService.getJobDetail(id));
        return "career/detail";
    }





    @GetMapping("/{id}/login-to-apply")
    public String loginToApply(@PathVariable Long id) {
        return "redirect:/jobs/" + id;
    }
}
