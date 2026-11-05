package com.group3.rms.application.controller;

import com.group3.rms.application.dto.ApplicationCreateRequest;
import com.group3.rms.application.service.CandidateApplicationService;
import com.group3.rms.common.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/jobs/{jobId}/apply")
@RequiredArgsConstructor
public class ApplyController {

    private final CandidateApplicationService candidateApplicationService;

    // SCR-15: Nộp hồ sơ (UC-18).
    @GetMapping
    public String applyForm(@PathVariable Long jobId, Model model, RedirectAttributes ra) {
        model.addAttribute("form", ApplicationCreateRequest.empty());
        return showForm(jobId, model, ra);
    }

    @PostMapping
    public String apply(
            @PathVariable Long jobId,
            @Valid @ModelAttribute("form") ApplicationCreateRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        try {
            if (!bindingResult.hasErrors()) {
                candidateApplicationService.apply(jobId, form);
                ra.addFlashAttribute("success", "Nộp hồ sơ thành công.");
                return "redirect:/my/applications";
            }
        } catch (BusinessException e) {
            // Lỗi không gắn với ô nào (đã nộp rồi, tin vừa đóng) → quay về SCR-14.
            if (e.getField() == null) {
                return backToJob(jobId, e, ra);
            }
            e.applyTo(bindingResult);
        }

        // Dữ liệu chưa hợp lệ → hiện lại form với lỗi dưới từng ô.
        return showForm(jobId, model, ra);
    }

    private String showForm(Long jobId, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("applyForm", candidateApplicationService.getApplyForm(jobId));
            return "career/apply";
        } catch (BusinessException e) {
            return backToJob(jobId, e, ra);
        }
    }

    private String backToJob(Long jobId, BusinessException e, RedirectAttributes ra) {
        ra.addFlashAttribute("error", e.getMessage());
        return "redirect:/jobs/" + jobId;
    }
}
