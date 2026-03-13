package com.patientagent.common.response;

/**
 * 统一 API 响应体封装。
 * <p>
 * 所有 REST 接口均以此格式返回，字段说明：
 * <ul>
 *   <li>{@code code} — 业务状态码，0 表示成功，非 0 为各类错误码。</li>
 *   <li>{@code message} — 人类可读的状态描述（成功为 "OK"，失败为错误说明）。</li>
 *   <li>{@code traceId} — 请求追踪 ID，可用于日志关联排查。</li>
 *   <li>{@code data} — 业务响应数据，失败时为 null。</li>
 * </ul>
 * </p>
 */
public class ApiResponse<T> {

    /** 业务状态码，0 = 成功。 */
    private int code;
    /** 操作结果描述。 */
    private String message;
    /** 请求追踪 ID，与日志中的 traceId 对应。 */
    private String traceId;
    /** 业务响应数据。 */
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, String traceId, T data) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String traceId, T data) {
        return new ApiResponse<>(0, "OK", traceId, data);
    }

    public static <T> ApiResponse<T> fail(String traceId, int code, String message) {
        return new ApiResponse<>(code, message, traceId, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
