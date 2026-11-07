package com.group3.rms.application.controller;

import com.group3.rms.application.dto.CvFileResponse;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.application.service.CandidateApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/my/applications")
@RequiredArgsConstructor
public class MyApplicationController {

    private final CandidateApplicationService candidateApplicationService;

    // SCR-16: Hồ sơ ứng tuyển của tôi (UC-19).
    @GetMapping
    public String myApplications(@RequestParam(required = false) String status, Model model) {
        model.addAttribute("list", candidateApplicationService.getMyApplications(parseStatus(status)));
        model.addAttribute("statuses", ApplicationStatus.values());
        return "application/my-list";
    }

    // UC-20: Rút hồ sơ. Vi phạm quy tắc → BusinessException, GlobalExceptionHandler đưa lỗi về trang trước.
    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id, RedirectAttributes ra) {
        candidateApplicationService.withdraw(id);
        ra.addFlashAttribute("success", "Đã rút hồ sơ.");
        return "redirect:/my/applications";
    }

    // Bổ sung ngoài đặc tả: xem lại CV đã nộp. PDF mở ngay trên trình duyệt, DOC/DOCX được tải về.
    @GetMapping("/{id}/cv")
    public ResponseEntity<Resource> viewCv(@PathVariable Long id) {
        CvFileResponse cv = candidateApplicationService.getMyCv(id);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(cv.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(cv.fileName()).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(cv.file());
    }

    /** Tab trạng thái bị sửa tay thành giá trị sai trên URL → hiện tab "Tất cả" thay vì trang lỗi. */
    private ApplicationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ApplicationStatus.valueOf(status.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
