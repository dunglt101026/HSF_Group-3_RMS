package com.group3.rms.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException e) {
        return createProblem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail handleValidation() {
        return createProblem(HttpStatus.BAD_REQUEST,
                "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại các thông tin đã nhập.");
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ProblemDetail handleForbidden(Exception e) {
        String message = e instanceof ForbiddenException
                ? e.getMessage() : "Bạn không có quyền truy cập trang hoặc thực hiện thao tác này.";
        return createProblem(HttpStatus.FORBIDDEN, message);
    }

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class, NoHandlerFoundException.class})
    public ProblemDetail handleNotFound() {
        return createProblem(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin bạn yêu cầu.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
        // Giữ status và header của Spring (ví dụ: thiếu tham số, sai HTTP method).
        if (e instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
            ProblemDetail problem = createProblem(error.getStatusCode(),
                    "Yêu cầu không hợp lệ. Vui lòng kiểm tra lại dữ liệu gửi lên.");
            return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders()).body(problem);
        }

        log.error("Lỗi không mong đợi khi xử lý API", e);
        ProblemDetail problem = createProblem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail createProblem(HttpStatusCode status, String message) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle("Không thể xử lý yêu cầu");
        return problem;
    }
}
