
package com.group3.rms.job.controller;

import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.job.dto.JobCreateRequest;
import com.group3.rms.job.dto.JobResponse;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.job.repository.DepartmentRepository;
import com.group3.rms.job.repository.SkillRepository;
import com.group3.rms.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/hr/jobs")
@RequiredArgsConstructor
public class HrJobController {

    private final JobService jobService;
    private final CurrentUserService currentUserService;
    private final DepartmentRepository departmentRepository;
    private final SkillRepository skillRepository;

    @GetMapping
    public String listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "true") boolean mine,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        CurrentUser currentUser = currentUserService.require();

        int currentPage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                currentPage,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Long ownerId = mine ? currentUser.id() : null;

        model.addAttribute(
                "jobs",
                jobService.searchJobs(keyword, status, ownerId, pageable)
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", JobStatus.values());
        model.addAttribute("mine", mine);
        model.addAttribute("currentUser", currentUser);

        return "job/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("form", new JobCreateRequest(
                null, null, null, null, null, null, null, null
        ));

        addFormOptions(model);

        model.addAttribute("pageTitle", "Tạo tin tuyển dụng");
        model.addAttribute("formAction", "/hr/jobs");

        return "job/form";
    }

    @GetMapping("/{id}")
    public String jobDetail(
            @PathVariable Long id,
            Model model
    ) {
        model.addAttribute("job", jobService.getJobDetail(id));
        return "job/detail";
    }

    private void addFormOptions(Model model) {
        model.addAttribute(
                "departments",
                departmentRepository.findAll(
                        Sort.by(Sort.Direction.ASC, "name")
                )
        );

        model.addAttribute(
                "skills",
                skillRepository.findAll(
                        Sort.by(Sort.Direction.ASC, "name")
                )
        );
    }

    @PostMapping
    public String createJob(
            @ModelAttribute JobCreateRequest request,
            RedirectAttributes redirectAttributes
    ) {
        JobResponse createdJob = jobService.createJob(request);

        redirectAttributes.addFlashAttribute(
                "success",
                "Tạo tin tuyển dụng thành công."
        );

        return "redirect:/hr/jobs/" + createdJob.id();
    }

}
