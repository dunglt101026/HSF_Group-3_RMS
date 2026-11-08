package com.group3.rms.common.web;

import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CurrentUserService currentUserService;

    @GetMapping("/")
    public String home(Model model, RedirectAttributes attributes) {

        Object error = model.getAttribute("error");
        if (error != null) {
            attributes.addFlashAttribute("error", error);
        }

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


    @RequestMapping("/error/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String error403() {
        return "error/403";
    }

    @GetMapping("/error/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String error404() {
        return "error/404";
    }

    @GetMapping("/error/500")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String error500() {
        return "error/500";
    }
}
