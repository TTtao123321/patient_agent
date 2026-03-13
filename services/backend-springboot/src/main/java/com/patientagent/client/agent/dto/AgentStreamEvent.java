package com.patientagent.client.agent.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * FastAPI Agent SSE 流中单一事件的封装。
 * <p>
 * 事件类型（{@code event}）可能的取值：
 * <ul>
 *   <li>{@code chunk} — 增量文本片段，{@code data.content} 存放该次增量内容。</li>
 *   <li>{@code done}  — 流结束，{@code data.answer} 存放完整回答。</li>
 *   <li>{@code error} — Agent 处理出错。</li>
 * </ul>
 * </p>
 */
public class AgentStreamEvent {

    /** SSE 事件类型（chunk / done / error 等）。 */
    private String event;
    /** SSE 事件模型数据，使用 JsonNode 保留原始结构以兼容未来扩展。 */
    private JsonNode data;

    public AgentStreamEvent() {
    }

    public AgentStreamEvent(String event, JsonNode data) {
        this.event = event;
        this.data = data;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }
}