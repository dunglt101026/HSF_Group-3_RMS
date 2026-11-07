package com.group3.rms.common.exception;

import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.common.web.HomeController;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ExceptionHandlingTest {

    private static final String INTERNAL_DETAIL = "internal-diagnostic";
    private static final String BUSINESS_MESSAGE = "Thao tác không hợp lệ.";

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");

        var templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        var viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");

        mvc = MockMvcBuilders.standaloneSetup(
                        new HomeController(mock(CurrentUserService.class)), new FailingPage(), new FailingApi())
                .setControllerAdvice(new GlobalExceptionHandler(), new ApiExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "/error/403, 403",
            "/error/404, 404",
            "/error/500, 500",
            "/failure/forbidden, 403",
            "/failure/missing, 404",
            "/failure/unexpected, 500",
            "/failure/status/403, 403",
            "/failure/status/404, 404",
            "/no-mapping, 404"
    })
    void rendersErrorPageWithItsHttpStatus(String path, int code) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().is(code))
                .andExpect(view().name("error/" + code))
                .andExpect(content().string(containsString(code + " — ")))
                .andExpect(content().string(not(containsString(INTERNAL_DETAIL))));
    }

    @Test
    void rendersForbiddenPageForForwardedPost() throws Exception {
        mvc.perform(post("/error/403").with(request -> {
                    request.setDispatcherType(DispatcherType.FORWARD);
                    return request;
                }))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"))
                .andExpect(content().string(containsString("403 — Không có quyền truy cập")));
    }

    @Test
    void returnsForbiddenProblemForApiAccessDenied() throws Exception {
        mvc.perform(get("/api/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(content().string(not(containsString(INTERNAL_DETAIL))));
    }

    @Test
    void redirectsToTheSameSiteRefererWithAFlashMessage() throws Exception {
        mvc.perform(post("/failure/business")
                        .header(HttpHeaders.REFERER, "http://localhost/admin/users?page=1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?page=1"))
                .andExpect(flash().attribute("error", BUSINESS_MESSAGE));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"http://other.example/admin/users", "http://localhost//other.example"})
    void fallsBackToHomeForAnUnsafeOrMissingReferer(String referer) throws Exception {
        var request = post("/failure/business");
        if (referer != null) {
            request.header(HttpHeaders.REFERER, referer);
        }

        mvc.perform(request)
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", BUSINESS_MESSAGE));
    }

    @Controller
    static class FailingPage {

        @GetMapping("/failure/forbidden")
        String forbidden() {
            throw new ForbiddenException(INTERNAL_DETAIL);
        }

        @GetMapping("/failure/missing")
        String missing() {
            throw new NotFoundException(INTERNAL_DETAIL);
        }

        @GetMapping("/failure/unexpected")
        String unexpected() {
            throw new IllegalStateException(INTERNAL_DETAIL);
        }

        @GetMapping("/failure/status/{code}")
        String httpError(@PathVariable int code) {
            throw new ResponseStatusException(HttpStatus.valueOf(code), INTERNAL_DETAIL);
        }

        @PostMapping("/failure/business")
        String business() {
            throw new BusinessException(BUSINESS_MESSAGE);
        }
    }

    @RestController
    static class FailingApi {

        @GetMapping("/api/forbidden")
        String forbidden() {
            throw new AccessDeniedException(INTERNAL_DETAIL);
        }
    }
}
