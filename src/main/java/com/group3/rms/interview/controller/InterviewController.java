package com.group3.rms.interview.controller;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.interview.dto.InterviewCreateRequest;
import com.group3.rms.interview.dto.InterviewEvaluationRequest;
import com.group3.rms.interview.dto.InterviewScheduledResponse;
import com.group3.rms.interview.dto.MyInterviewTab;
import com.group3.rms.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;


    @GetMapping("/applications/{applicationId}/interviews/new")
    public String scheduleForm(@PathVariable Long applicationId, Model model) {
        model.addAttribute("scheduleForm", interviewService.getScheduleForm(applicationId));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", InterviewCreateRequest.empty());
        }
        return "interview/form";
    }

    @PostMapping("/applications/{applicationId}/interviews/new")
    public String schedule(
            @PathVariable Long applicationId,
            @Valid @ModelAttribute("form") InterviewCreateRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("scheduleForm", interviewService.getScheduleForm(applicationId));
            return "interview/form";
        }

        try {
            InterviewScheduledResponse result = interviewService.scheduleInterview(applicationId, form);
            ra.addFlashAttribute("success",
                    "Đã lên lịch vòng " + result.roundNo() + " với " + result.interviewerName() + ".");
            return "redirect:/applications/" + applicationId;
        } catch (BusinessException e) {
            e.applyTo(bindingResult);
            model.addAttribute("scheduleForm", interviewService.getScheduleForm(applicationId));
            return "interview/form";
        }
    }


    @GetMapping("/interviews/my")
    public String mySchedule(
            @RequestParam(required = false) String tab,
            Model model
    ) {
        model.addAttribute("schedule", interviewService.getMySchedule(parseTab(tab)));
        return "interview/my-schedule";
    }


    private static MyInterviewTab parseTab(String tab) {
        if (tab != null) {
            for (MyInterviewTab candidate : MyInterviewTab.values()) {
                if (candidate.name().equalsIgnoreCase(tab.trim())) {
                    return candidate;
                }
            }
        }
        return MyInterviewTab.UPCOMING;
    }


    @GetMapping("/interviews/{id}/evaluation")
    public String evaluationForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("evaluation", interviewService.getEvaluation(id));
        } catch (BusinessException e) {

            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/interviews/my";
        }
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", InterviewEvaluationRequest.empty());
        }
        return "interview/evaluation";
    }

    @PostMapping("/interviews/{id}/evaluation")
    public String submitEvaluation(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") InterviewEvaluationRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        try {
            if (bindingResult.hasErrors()) {
                model.addAttribute("evaluation", interviewService.getEvaluation(id));
                return "interview/evaluation";
            }
            interviewService.submitEvaluation(id, form);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/interviews/my";
        }

        ra.addFlashAttribute("success", "Đã gửi đánh giá.");
        return "redirect:/interviews/my?tab=EVALUATED";
    }
}
