package com.group3.rms.job.controller;

import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.job.dto.JobCreateRequest;
import com.group3.rms.job.dto.JobResponse;
import com.group3.rms.job.dto.JobUpdateRequest;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.job.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;
import java.util.Set;

@Controller
@RequestMapping("/hr/jobs")
@RequiredArgsConstructor
public class HrJobController {

    private static final int PAGE_SIZE = 10;

    private final JobService jobService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "true") boolean mine,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        JobStatus selectedStatus = parseStatus(status, JobStatus.class, model);
        CurrentUser currentUser = currentUserService.require();
        Long ownerId = mine ? currentUser.id() : null;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        model.addAttribute("jobs", jobService.searchJobs(keyword, selectedStatus, ownerId, pageable));
        model.addAttribute("statusCounts", jobService.countJobsByStatus(keyword, ownerId));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("mine", mine);

        return "job/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        return renderCreateForm(model, new JobCreateRequest(
                null, null, null, null, null, null, null, Set.of()
        ));
    }

    @GetMapping("/{id}")
    public String jobDetail(@PathVariable Long id, Model model) {
        model.addAttribute("job", jobService.getJobDetail(id));
        model.addAttribute("stats", jobService.getApplicationStats(id));

        return "job/detail";
    }

    @GetMapping("/{id}/applications")
    public String jobApplications(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        ApplicationStatus selectedStatus = parseStatus(status, ApplicationStatus.class, model);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        model.addAttribute("job", jobService.getJobDetail(id));
        model.addAttribute("stats", jobService.getApplicationStats(id));
        model.addAttribute("applications", jobService.searchApplications(id, selectedStatus, pageable));
        model.addAttribute("selectedStatus", selectedStatus);

        return "job/applications";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        JobResponse job = jobService.getJobForEdit(id);

        return renderEditForm(model, job, toUpdateRequest(job));
    }

    @PostMapping
    public String createJob(
            @Valid @ModelAttribute("form") JobCreateRequest form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!result.hasErrors()) {
            try {
                JobResponse createdJob = jobService.createJob(form);

                redirectAttributes.addFlashAttribute(
                        "success",
                        "Đã lưu tin ở trạng thái Nháp."
                );

                return "redirect:/hr/jobs/" + createdJob.id();
            } catch (BusinessException e) {
                // Lỗi nghiệp vụ gắn với một ô: hiện dưới ô, giữ dữ liệu và kỹ năng đã chọn.
                e.applyTo(result);
            }
        }

        return renderCreateForm(model, form);
    }

    @PostMapping("/{id}/edit")
    public String updateJob(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") JobUpdateRequest form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            // Kiểm tra quyền trước khi trả lại form: non-owner → 403, tin CLOSED → thông báo.
            return renderEditForm(model, jobService.getJobForEdit(id), form);
        }

        try {
            jobService.updateJob(id, form);
        } catch (BusinessException e) {
            e.applyTo(result);
            // updateJob đã kiểm tra quyền và trạng thái trước khi ném lỗi nghiệp vụ.
            return renderEditForm(model, jobService.getJobDetail(id), form);
        }

        redirectAttributes.addFlashAttribute("success", "Đã cập nhật tin.");

        return "redirect:/hr/jobs/" + id;
    }

    @PostMapping("/{id}/publish")
    public String publishJob(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        jobService.publishJob(id);

        redirectAttributes.addFlashAttribute("success", "Đã xuất bản tin tuyển dụng.");

        return "redirect:/hr/jobs/" + id;
    }

    @PostMapping("/{id}/close")
    public String closeJob(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        jobService.closeJob(id);

        redirectAttributes.addFlashAttribute("success", "Đã đóng tin tuyển dụng.");

        return "redirect:/hr/jobs/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteJob(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        jobService.deleteJob(id);

        redirectAttributes.addFlashAttribute("success", "Đã xóa tin tuyển dụng.");

        return "redirect:/hr/jobs";
    }

    private String renderCreateForm(Model model, JobCreateRequest form) {
        model.addAttribute("form", form);
        model.addAttribute("formOptions", jobService.getFormOptions());
        model.addAttribute("pageTitle", "Tạo tin tuyển dụng");
        model.addAttribute("formAction", "/hr/jobs");

        return "job/form";
    }

    private String renderEditForm(Model model, JobResponse job, JobUpdateRequest form) {
        model.addAttribute("form", form);
        model.addAttribute("job", job);
        model.addAttribute("formOptions", jobService.getFormOptions());
        model.addAttribute("pageTitle", "Chỉnh sửa tin tuyển dụng");
        model.addAttribute("formAction", "/hr/jobs/" + job.id() + "/edit");

        return "job/form";
    }

    private JobUpdateRequest toUpdateRequest(JobResponse job) {
        return new JobUpdateRequest(
                job.title(),
                job.description(),
                job.location(),
                job.salaryMin(),
                job.salaryMax(),
                job.deadline(),
                job.departmentId(),
                Set.copyOf(job.skillIds())
        );
    }

    private <T extends Enum<T>> T parseStatus(String value, Class<T> statusType, Model model) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Enum.valueOf(statusType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            model.addAttribute("statusFilterError", "Trạng thái lọc không hợp lệ; đã bỏ qua bộ lọc.");
            return null;
        }
    }
}
