package com.patientagent.modules.chat.dto;

import java.util.List;

/**
 * 会话列表分页响应。
 */
public class ChatSessionListResponse {

    private int page;
    private int pageSize;
    private long total;
    private List<ChatSessionItemResponse> items;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public List<ChatSessionItemResponse> getItems() { return items; }
    public void setItems(List<ChatSessionItemResponse> items) { this.items = items; }
}
