package com.patientagent.modules.chat.dto;

import java.util.List;

public class ChatHistoryResponse {

    private String sessionNo;
    private int page;
    private int pageSize;
    private long total;
    private List<ChatMessageItemResponse> items;

    public String getSessionNo() {
        return sessionNo;
    }

    public void setSessionNo(String sessionNo) {
        this.sessionNo = sessionNo;
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

    public List<ChatMessageItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ChatMessageItemResponse> items) {
        this.items = items;
    }
}
