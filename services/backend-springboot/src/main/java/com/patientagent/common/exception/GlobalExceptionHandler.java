package com.patientagent.common.exception;

import com.patientagent.common.response.ApiResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * 全局异常处理器，拦截所有 Controller 抛出的未捕获异常并封装为统一 {@link ApiResponse} 返回。
 * <p>
 * 处理策略：
 * <ul>
 *   <li>参数校验失败（{@link MethodArgumentNotValidException}）→ HTTP 200 + code=40001 + 第一个字段错误信息。</li>
 *   <li>业务参数错误（{@link IllegalArgumentException}）→ HTTP 200 + code=40001。</li>
 *   <li>业务状态错误（{@link IllegalStateException}）→ HTTP 200 + code=50001。</li>
 *   <li>未知异常→ HTTP 200 + code=50001。</li>
 * </ul>
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求体参数校验失败，反馈第一个字段的错误信息。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Invalid parameters"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ApiResponse.fail(traceId, 40001, message);
    }

    /**
     * 处理业务参数错误，通常由服务层主动抛出。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.fail(traceId, 40001, ex.getMessage());
    }

    /**
     * 处理业务状态错误，如 Agent 服务返回异常、密码哈希失败等。
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<Void> handleIllegalState(IllegalStateException ex) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.fail(traceId, 50001, ex.getMessage());
    }

    /**
     * 兼容其它未预期异常，防止堆栈信息泄露。
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleGeneric(Exception ex) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.fail(traceId, 50001, ex.getMessage());
    }
}
