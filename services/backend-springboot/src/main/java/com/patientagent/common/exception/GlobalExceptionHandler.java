package com.patientagent.common.exception;

import com.patientagent.common.response.ApiResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Invalid parameters"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ApiResponse.fail(traceId, 40001, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.fail(traceId, 40001, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<Void> handleIllegalState(IllegalStateException ex) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.fail(traceId, 50001, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleGeneric(Exception ex) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.fail(traceId, 50001, ex.getMessage());
    }
}
