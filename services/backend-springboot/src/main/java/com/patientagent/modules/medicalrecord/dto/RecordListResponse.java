package com.patientagent.modules.medicalrecord.dto;

import java.util.List;

/**
 * 医疗记录列表分页响应 DTO。
 */
public class RecordListResponse {

    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    private Long totalElements;
    private List<RecordItemResponse> items;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public List<RecordItemResponse> getItems() {
        return items;
    }

    public void setItems(List<RecordItemResponse> items) {
        this.items = items;
    }
}
