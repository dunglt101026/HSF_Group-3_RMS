package com.group3.rms.common.web;

import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Trang chủ "/": Guest và Candidate sang trang việc làm, nhân viên về trang theo vai trò. */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CurrentUserService currentUserService;

    @GetMapping("/")
    public String home() {
        String target = currentUserService.get()
                .map(CurrentUser::role)
                .map(role -> switch (role) {
                    case ADMIN -> "/admin/users";
                    case HR -> "/hr/jobs";
                    case INTERVIEWER -> "/interviews/my";
                    case CANDIDATE -> "/jobs";
                })
                .orElse("/jobs");
        return "redirect:" + target;
    }
}
