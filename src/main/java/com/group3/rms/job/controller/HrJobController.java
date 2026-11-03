
package com.group3.rms.job.controller;

import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/hr/jobs")
@RequiredArgsConstructor
public class HrJobController {

    private final JobService jobService;

    // SCR-09: Danh sách tin tuyển dụng.
    @GetMapping
    public String listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        // Không cho page âm.
        int currentPage = Math.max(page, 0);

        // Mỗi trang hiển thị 10 tin, mới nhất trước.
        Pageable pageable = PageRequest.of(
                currentPage,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // Tạm thời ownerId = null: xem tất cả tin.
        // Chờ CurrentUserService của TV1 để lọc tin HR phụ trách.
        model.addAttribute(
                "jobs",
                jobService.searchJobs(keyword, status, null, pageable)
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", JobStatus.values());

        return "job/list";
    }

    // SCR-11: Chi tiết tin tuyển dụng.
    @GetMapping("/{id}")
    public String jobDetail(
            @PathVariable Long id,
            Model model
    ) {
        model.addAttribute("job", jobService.getJobDetail(id));
        return "job/detail";
    }
}
