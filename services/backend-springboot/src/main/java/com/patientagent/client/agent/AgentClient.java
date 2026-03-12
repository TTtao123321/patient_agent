package com.patientagent.client.agent;

public interface AgentClient {

    String chat(String sessionNo, Long userId, String message);
}
