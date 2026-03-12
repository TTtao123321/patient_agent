package com.patientagent.common.response;

public class ApiResponse<T> {

    private int code;
    private String message;
    private String traceId;
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
