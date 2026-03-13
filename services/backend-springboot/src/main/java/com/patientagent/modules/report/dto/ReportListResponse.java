package com.patientagent.modules.report.dto;

import java.util.List;

/**
 * 报告列表分页响应 DTO。
 */
public class ReportListResponse {

    private Long userId;
    private int page;
    private int pageSize;
    private long total;
    private List<ReportItemResponse> items;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<ReportItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ReportItemResponse> items) {
        this.items = items;
    }
}
