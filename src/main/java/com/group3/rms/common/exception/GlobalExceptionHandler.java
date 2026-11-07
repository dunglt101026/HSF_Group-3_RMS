package com.group3.rms.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.view.json.JacksonJsonView;

import java.net.URI;
import java.net.URISyntaxException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusiness(BusinessException e, HttpServletRequest request, RedirectAttributes attributes) {
        if (isApiRequest(request)) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request);
        }

        attributes.addFlashAttribute("error", e.getMessage());
        return new ModelAndView("redirect:" + sameSiteReferer(request));
    }

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class, NoHandlerFoundException.class})
    public ModelAndView handleNotFound(HttpServletRequest request) {
        return errorResponse(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin bạn yêu cầu.", request);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ModelAndView handleForbidden(Exception e, HttpServletRequest request) {
        String message = (e instanceof ForbiddenException) ? e.getMessage() : "Bạn không có quyền truy cập trang hoặc thực hiện thao tác này.";
        return errorResponse(HttpStatus.FORBIDDEN, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatusCode clientErrorStatus = null;
        // ResponseStatusException cũng thuộc ErrorResponse.
        if (e instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
            clientErrorStatus = error.getStatusCode();
            error.getHeaders().forEach((name, values) -> {
                for (String value : values) {
                    response.addHeader(name, value);
                }
            });
        } else if (e instanceof HttpMessageNotReadableException
                || e instanceof MethodArgumentTypeMismatchException) {
            clientErrorStatus = HttpStatus.BAD_REQUEST;
        }

        if (clientErrorStatus != null) {
            if (isApiRequest(request)
                    || clientErrorStatus.isSameCodeAs(HttpStatus.FORBIDDEN)
                    || clientErrorStatus.isSameCodeAs(HttpStatus.NOT_FOUND)) {
                return errorResponse(clientErrorStatus,
                        "Yêu cầu không hợp lệ. Vui lòng kiểm tra lại dữ liệu gửi lên.", request);
            }

            // Kết thúc phản hồi với status gốc, không đưa chi tiết exception ra trang lỗi mặc định.
            response.setStatus(clientErrorStatus.value());
            return new ModelAndView();
        }

        log.error("Lỗi không mong đợi", e);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.", request);
    }

    private ModelAndView errorResponse(HttpStatusCode status, String message, HttpServletRequest request) {
        if (isApiRequest(request)) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
            problem.setTitle("Không thể xử lý yêu cầu");
            problem.setInstance(URI.create(request.getRequestURI()));

            JacksonJsonView view = new JacksonJsonView();
            view.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            // Chỉ xuất thông tin lỗi, không đưa các thuộc tính model khác vào JSON.
            view.setModelKey("problem");
            view.setExtractValueFromSingleKeyModel(true);

            ModelAndView modelAndView = new ModelAndView(view);
            modelAndView.setStatus(status);
            modelAndView.addObject("problem", problem);
            return modelAndView;
        }

        return new ModelAndView("error/" + status.value(), status);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String apiPath = request.getContextPath() + "/api";
        String path = request.getRequestURI();
        return path.equals(apiPath) || path.startsWith(apiPath + "/");
    }

    private String sameSiteReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }

        try {
            URI uri = new URI(referer);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return "/";
            }
            if (!request.getScheme().equalsIgnoreCase(scheme)
                    || !request.getServerName().equalsIgnoreCase(uri.getHost())
                    || uri.getRawUserInfo() != null) {
                return "/";
            }

            int port = uri.getPort();
            if (port == -1) {
                port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
            }
            if (port != request.getServerPort()) {
                return "/";
            }

            String path = uri.getRawPath();
            String contextPath = request.getContextPath();
            if (path == null || !(path.equals(contextPath) || path.startsWith(contextPath + "/"))) {
                return "/";
            }

            // Spring tự thêm context path khi xử lý redirect bắt đầu bằng "/".
            path = path.substring(contextPath.length());
            if (path.isEmpty() || path.equals("/")) {
                return "/";
            }
            String decodedPath = new URI(path).getPath();
            if (!path.startsWith("/") || path.startsWith("//")
                    || decodedPath == null || !decodedPath.startsWith("/") || decodedPath.startsWith("//")
                    || decodedPath.contains("\\") || decodedPath.contains(";")
                    || decodedPath.chars().anyMatch(Character::isISOControl)) {
                return "/";
            }
            // Chặn đường dẫn đi ra ngoài ứng dụng, kể cả khi dấu chấm được mã hóa.
            for (String part : decodedPath.split("/")) {
                if (part.equals(".") || part.equals("..")) {
                    return "/";
                }
            }

            String query = uri.getRawQuery();
            if (query != null) {
                return path + "?" + query;
            }
            return path;
        } catch (URISyntaxException e) {
            // Referer sai định dạng thì về trang chủ.
            return "/";
        }
    }
}
