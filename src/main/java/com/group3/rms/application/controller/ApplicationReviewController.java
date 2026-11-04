package com.group3.rms.application.controller;

import com.group3.rms.application.service.ApplicationReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // UC-22: Duyệt hồ sơ. Vi phạm quy tắc → BusinessException, GlobalExceptionHandler đưa lỗi về trang trước.
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        applicationReviewService.approve(id);
        ra.addFlashAttribute("success", "Đã duyệt hồ sơ, chuyển sang vòng phỏng vấn.");
        return "redirect:/applications/" + id;
    }

    // UC-22: Loại hồ sơ.
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes ra) {
        applicationReviewService.reject(id);
        ra.addFlashAttribute("success", "Đã loại hồ sơ.");
        return "redirect:/applications/" + id;
    }

    // UC-22: Tuyển.
    @PostMapping("/{id}/hire")
    public String hire(@PathVariable Long id, RedirectAttributes ra) {
        applicationReviewService.hire(id);
        ra.addFlashAttribute("success", "Đã chuyển hồ sơ sang Đã tuyển.");
        return "redirect:/applications/" + id;
    }
}
