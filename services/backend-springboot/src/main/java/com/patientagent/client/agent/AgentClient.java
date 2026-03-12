package com.patientagent.client.agent;

import com.patientagent.client.agent.dto.AgentStreamEvent;

import java.util.function.Consumer;

public interface AgentClient {

    String chat(String sessionNo, Long userId, String message);

    void streamChat(String sessionNo, Long userId, String message, Consumer<AgentStreamEvent> eventConsumer);
}
