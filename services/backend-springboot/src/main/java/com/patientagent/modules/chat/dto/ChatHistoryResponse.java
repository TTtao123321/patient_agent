package com.patientagent.modules.chat.dto;

import java.util.List;

/**
 * 聊天历史分页查询响应实体。
 * <p>
 * 返回指定会话的讯息分页列表，配合 {@code total} 字段可实现前端分页展示。
 * 消息按序列号升序排列，代表对话流时水顺序。
 * </p>
 */
public class ChatHistoryResponse {

    /** 会话编号。 */
    private String sessionNo;
    /** 当前页码（从 1 开始）。 */
    private int page;
    /** 每页条数。 */
    private int pageSize;
    /** 该会话历史消息总条数。 */
    private long total;
    /** 当前页的消息列表。 */
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
