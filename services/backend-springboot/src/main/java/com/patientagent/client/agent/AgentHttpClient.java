package com.patientagent.client.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patientagent.client.agent.dto.AgentChatRequest;
import com.patientagent.client.agent.dto.AgentStreamEvent;
import com.patientagent.config.AgentProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

@Component
public class AgentHttpClient implements AgentClient {

    private final RestTemplate restTemplate;
    private final AgentProperties agentProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AgentHttpClient(RestTemplate restTemplate, AgentProperties agentProperties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.agentProperties = agentProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
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

    @Override
    public void streamChat(String sessionNo, Long userId, String message, Consumer<AgentStreamEvent> eventConsumer) {
        String url = normalizeBaseUrl(agentProperties.getBaseUrl()) + "/agent/chat/stream";

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionNo(sessionNo);
        request.setUserId(userId);
        request.setQuery(message);

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofMinutes(5))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Agent stream service returned invalid status: " + response.statusCode());
            }

            parseSseStream(response, eventConsumer);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read agent stream: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Agent stream request interrupted", ex);
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

    private void parseSseStream(
            HttpResponse<java.io.InputStream> response,
            Consumer<AgentStreamEvent> eventConsumer
    ) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8)
        )) {
            String currentEvent = "message";
            StringBuilder dataBuffer = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    dispatchSseEvent(currentEvent, dataBuffer, eventConsumer);
                    currentEvent = "message";
                    dataBuffer.setLength(0);
                    continue;
                }

                if (line.startsWith("event:")) {
                    currentEvent = line.substring("event:".length()).trim();
                    continue;
                }

                if (line.startsWith("data:")) {
                    if (dataBuffer.length() > 0) {
                        dataBuffer.append('\n');
                    }
                    dataBuffer.append(line.substring("data:".length()).trim());
                }
            }

            dispatchSseEvent(currentEvent, dataBuffer, eventConsumer);
        }
    }

    private void dispatchSseEvent(
            String eventName,
            StringBuilder dataBuffer,
            Consumer<AgentStreamEvent> eventConsumer
    ) throws IOException {
        if (dataBuffer.length() == 0) {
            return;
        }

        JsonNode dataNode = objectMapper.readTree(dataBuffer.toString());
        eventConsumer.accept(new AgentStreamEvent(eventName, dataNode));
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
