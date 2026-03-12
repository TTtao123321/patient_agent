package com.patientagent.client.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.patientagent.client.agent.dto.AgentChatRequest;
import com.patientagent.config.AgentProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class AgentHttpClient implements AgentClient {

    private final RestTemplate restTemplate;
    private final AgentProperties agentProperties;

    public AgentHttpClient(RestTemplate restTemplate, AgentProperties agentProperties) {
        this.restTemplate = restTemplate;
        this.agentProperties = agentProperties;
    }

    @Override
    public String chat(String sessionNo, Long userId, String message) {
        String url = normalizeBaseUrl(agentProperties.getBaseUrl()) + "/agent/chat";

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionNo(sessionNo);
        request.setUserId(userId);
        request.setQuery(message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    JsonNode.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Agent service returned invalid response");
            }

            return parseAnswer(response.getBody());
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to call agent service: " + ex.getMessage(), ex);
        }
    }

    private String parseAnswer(JsonNode body) {
        if (body.hasNonNull("answer")) {
            return body.get("answer").asText();
        }
        if (body.has("data") && body.get("data").hasNonNull("answer")) {
            return body.get("data").get("answer").asText();
        }
        throw new IllegalStateException("Agent response does not contain answer field");
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("agent.base-url is not configured");
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
