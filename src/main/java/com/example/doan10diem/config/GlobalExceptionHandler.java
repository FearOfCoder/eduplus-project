package com.example.doan10diem.config;

import com.example.doan10diem.dto.RegisterDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Component
@ControllerAdvice
@Order(1)  // Đảm bảo handler này được ưu tiên
public class GlobalExceptionHandler {

    @ExceptionHandler(LazyInitializationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleLazyInitializationException(LazyInitializationException e, HttpServletRequest request) {
        log.error("LazyInitializationException caught: ", e);
        return buildErrorModelAndView(
            "Lỗi lazy loading: " + (e.getMessage() != null ? e.getMessage() : "Không thể load dữ liệu từ database"),
            e,
            request,
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.error("IllegalArgumentException caught: ", e);
        return buildErrorModelAndView("Dữ liệu không hợp lệ: " + (e.getMessage() != null ? e.getMessage() : ""), e, request, HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.error("AccessDeniedException caught: ", e);
        return buildErrorModelAndView("Bạn không có quyền truy cập: " + (e.getMessage() != null ? e.getMessage() : ""), e, request, HttpStatus.FORBIDDEN.value());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.error("DataIntegrityViolationException caught: ", e);
        return buildErrorModelAndView("Lỗi dữ liệu: " + (e.getMessage() != null ? e.getMessage() : "Dữ liệu không hợp lệ hoặc đã tồn tại"), e, request, HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleMultipartException(MultipartException e, HttpServletRequest request) {
        log.error("MultipartException caught: ", e);
        String message = "Lỗi upload file. Vui lòng kiểm tra lại: " + (e.getMessage() != null ? e.getMessage() : "File quá lớn hoặc định dạng không hợp lệ");
        ModelAndView mav = new ModelAndView("auth/register");
        mav.addObject("error", message);
        mav.addObject("registerDTO", new RegisterDTO());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleException(Exception e, HttpServletRequest request) {
        log.error("Exception caught in GlobalExceptionHandler: ", e);
        String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return buildErrorModelAndView("Lỗi: " + errorMessage, e, request, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private ModelAndView buildErrorModelAndView(String errorMessage, Exception e, HttpServletRequest request, int statusCode) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("error", errorMessage);
        mav.addObject("exceptionType", e.getClass().getName());
        mav.addObject("status", statusCode);
        mav.addObject("requestUri", request != null ? request.getRequestURI() : "N/A");
        
        StringBuilder stackTrace = new StringBuilder();
        if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
            for (int i = 0; i < Math.min(e.getStackTrace().length, 100); i++) {
                stackTrace.append(e.getStackTrace()[i].toString()).append("\n");
            }
        }
        mav.addObject("stackTrace", stackTrace.length() > 0 ? stackTrace.toString() : null);
        return mav;
    }
}
