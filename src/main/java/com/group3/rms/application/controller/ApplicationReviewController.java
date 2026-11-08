package com.group3.rms.application.controller;

import com.group3.rms.application.dto.CvFileResponse;
import com.group3.rms.application.service.ApplicationReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationReviewController {

    private final ApplicationReviewService applicationReviewService;


    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("app", applicationReviewService.getDetail(id));
        return "application/detail";
    }


    @GetMapping("/{id}/cv")
    public ResponseEntity<Resource> downloadCv(@PathVariable Long id) {
        CvFileResponse cv = applicationReviewService.downloadCv(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(cv.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(cv.file());
    }


    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        applicationReviewService.approve(id);
        ra.addFlashAttribute("success", "Đã duyệt hồ sơ, chuyển sang vòng phỏng vấn.");
        return "redirect:/applications/" + id;
    }


    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes ra) {
        applicationReviewService.reject(id);
        ra.addFlashAttribute("success", "Đã loại hồ sơ.");
        return "redirect:/applications/" + id;
    }


    @PostMapping("/{id}/hire")
    public String hire(@PathVariable Long id, RedirectAttributes ra) {
        applicationReviewService.hire(id);
        ra.addFlashAttribute("success", "Đã chuyển hồ sơ sang Đã tuyển.");
        return "redirect:/applications/" + id;
    }
}
