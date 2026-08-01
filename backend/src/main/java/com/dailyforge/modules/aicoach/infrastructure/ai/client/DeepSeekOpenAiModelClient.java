package com.dailyforge.modules.aicoach.infrastructure.ai.client;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiCoachProperties;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiChatMessage;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiModelRequest;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiModelResponse;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiToolCall;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiToolDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class DeepSeekOpenAiModelClient implements AiModelClient {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Logger log = LoggerFactory.getLogger(DeepSeekOpenAiModelClient.class);
    private static final int LOG_PREVIEW_LIMIT = 320;

    private final AiCoachProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public DeepSeekOpenAiModelClient(
            AiCoachProperties properties,
            ObjectMapper objectMapper,
            List<RestClientCustomizer> customizers) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        RestClient.Builder builder = RestClient.builder();
        for (RestClientCustomizer customizer : customizers) {
            customizer.customize(builder);
        }
        this.restClientBuilder = builder;
    }

    @Override
    public AiModelResponse generate(AiModelRequest request) {
        assertConfigured();
        Duration timeout = request.timeout() != null ? request.timeout() : properties.getTimeout();
        String model = StringUtils.hasText(request.model()) ? request.model() : properties.getModel();
        RestClient client = restClientBuilder
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey().trim())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(buildRequestFactory(timeout))
                .build();
        String requestSummary = summarizeRequest(request, model, timeout);
        try {
            JsonNode response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildPayload(request))
                    .retrieve()
                    .body(JsonNode.class);
            return toResponse(response);
        } catch (ResourceAccessException exception) {
            if (isTimeoutException(exception)) {
                String message = "deepseek request timed out after " + timeout;
                log.warn(
                        "DeepSeek request timeout. taskId={}, taskType={}, stage={}, summary={}, rootCause={}",
                        request.taskId(),
                        request.taskType(),
                        request.stage(),
                        requestSummary,
                        summarizeThrowable(exception));
                throw new BusinessException(ErrorCode.AI_SERVICE_TIMEOUT, message);
            }
            String message = "deepseek network access failed";
            log.warn(
                    "DeepSeek request network failure. taskId={}, taskType={}, stage={}, summary={}, rootCause={}",
                    request.taskId(),
                    request.taskType(),
                    request.stage(),
                    requestSummary,
                    summarizeThrowable(exception));
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, message);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            String message = buildHttpFailureMessage(status);
            log.warn(
                    "DeepSeek request http failure. taskId={}, taskType={}, stage={}, status={}, summary={}, responsePreview={}, rootCause={}",
                    request.taskId(),
                    request.taskType(),
                    request.stage(),
                    status,
                    requestSummary,
                    abbreviate(exception.getResponseBodyAsString()),
                    summarizeThrowable(exception));
            if (status == 408) {
                throw new BusinessException(ErrorCode.AI_SERVICE_TIMEOUT, message);
            }
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, message);
        } catch (RestClientException exception) {
            if (isTimeoutException(exception)) {
                String message = "deepseek request timed out after " + timeout;
                log.warn(
                        "DeepSeek request timeout after client failure. taskId={}, taskType={}, stage={}, summary={}, rootCause={}",
                        request.taskId(),
                        request.taskType(),
                        request.stage(),
                        requestSummary,
                        summarizeThrowable(exception));
                throw new BusinessException(ErrorCode.AI_SERVICE_TIMEOUT, message);
            }
            String message = "deepseek request failed before receiving a valid response";
            log.warn(
                    "DeepSeek request client failure. taskId={}, taskType={}, stage={}, summary={}, rootCause={}",
                    request.taskId(),
                    request.taskType(),
                    request.stage(),
                    requestSummary,
                    summarizeThrowable(exception));
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, message);
        }
    }

    private SimpleClientHttpRequestFactory buildRequestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = (int) Math.max(1000L, timeout.toMillis());
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return factory;
    }

    private Map<String, Object> buildPayload(AiModelRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", StringUtils.hasText(request.model()) ? request.model() : properties.getModel());
        payload.put("messages", buildMessages(request.messages()));
        if (request.tools() != null && !request.tools().isEmpty()) {
            payload.put("tools", buildTools(request.tools()));
            payload.put("tool_choice", "auto");
        }
        return payload;
    }

    private List<Map<String, Object>> buildMessages(List<AiChatMessage> messages) {
        List<Map<String, Object>> value = new ArrayList<>();
        for (AiChatMessage message : messages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", message.role());
            if (StringUtils.hasText(message.toolCallId())) {
                item.put("tool_call_id", message.toolCallId());
            }
            if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
                List<Map<String, Object>> toolCalls = new ArrayList<>();
                for (AiToolCall toolCall : message.toolCalls()) {
                    Map<String, Object> function = new LinkedHashMap<>();
                    function.put("name", toolCall.name());
                    function.put("arguments", toolCall.argumentsJson());
                    Map<String, Object> toolCallValue = new LinkedHashMap<>();
                    toolCallValue.put("id", toolCall.id());
                    toolCallValue.put("type", "function");
                    toolCallValue.put("function", function);
                    toolCalls.add(toolCallValue);
                }
                item.put("tool_calls", toolCalls);
            }
            item.put("content", message.content());
            value.add(item);
        }
        return value;
    }

    private List<Map<String, Object>> buildTools(List<AiToolDefinition> tools) {
        List<Map<String, Object>> value = new ArrayList<>();
        for (AiToolDefinition tool : tools) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.put("parameters", objectMapper.convertValue(tool.parametersSchema(), MAP_TYPE));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "function");
            item.put("function", function);
            value.add(item);
        }
        return value;
    }

    private AiModelResponse toResponse(JsonNode response) {
        JsonNode message = response.path("choices").path(0).path("message");
        if (message.isMissingNode() || message.isNull()) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "ai response message is missing");
        }
        List<AiToolCall> toolCalls = new ArrayList<>();
        JsonNode toolCallsNode = message.path("tool_calls");
        if (toolCallsNode.isArray()) {
            for (JsonNode toolCallNode : toolCallsNode) {
                JsonNode functionNode = toolCallNode.path("function");
                String id = toolCallNode.path("id").asText(null);
                String name = functionNode.path("name").asText(null);
                JsonNode argumentsNode = functionNode.path("arguments");
                String argumentsJson = argumentsNode.isTextual()
                        ? argumentsNode.asText()
                        : argumentsNode.toString();
                if (!StringUtils.hasText(id) || !StringUtils.hasText(name) || !StringUtils.hasText(argumentsJson)) {
                    throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "ai tool call payload is invalid");
                }
                toolCalls.add(new AiToolCall(id, name, argumentsJson));
            }
        }
        String finishReason = response.path("choices").path(0).path("finish_reason").asText(null);
        JsonNode contentNode = message.path("content");
        String content = contentNode.isMissingNode() || contentNode.isNull() ? null : contentNode.asText();
        if (!toolCalls.isEmpty()) {
            return new AiModelResponse(content, toolCalls, finishReason);
        }
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "ai response content is empty");
        }
        return new AiModelResponse(content, List.of(), finishReason);
    }

    private void assertConfigured() {
        if (!StringUtils.hasText(properties.getBaseUrl())
                || !StringUtils.hasText(properties.getModel())
                || !StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "ai provider config is incomplete");
        }
    }

    private boolean isTimeoutException(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (StringUtils.hasText(message) && message.toLowerCase().contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String summarizeRequest(AiModelRequest request, String model, Duration timeout) {
        int messageCount = request.messages() == null ? 0 : request.messages().size();
        int totalChars = 0;
        if (request.messages() != null) {
            for (AiChatMessage message : request.messages()) {
                if (message.content() != null) {
                    totalChars += message.content().length();
                }
            }
        }
        List<String> toolNames = new ArrayList<>();
        if (request.tools() != null) {
            for (AiToolDefinition tool : request.tools()) {
                toolNames.add(tool.name());
                if (toolNames.size() >= 6) {
                    break;
                }
            }
        }
        return "model=" + model
                + ", timeout=" + timeout
                + ", messages=" + messageCount
                + ", messageChars=" + totalChars
                + ", tools=" + (request.tools() == null ? 0 : request.tools().size())
                + ", toolNames=" + toolNames;
    }

    private String summarizeThrowable(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String type = root.getClass().getSimpleName();
        String message = abbreviate(root.getMessage());
        if (root instanceof UnknownHostException) {
            return "UnknownHostException: " + message;
        }
        if (root instanceof ConnectException) {
            return "ConnectException: " + message;
        }
        return type + ": " + message;
    }

    private String buildHttpFailureMessage(int status) {
        if (status >= 400 && status < 500) {
            return "deepseek request rejected with http " + status;
        }
        return "deepseek service returned http " + status;
    }

    private String abbreviate(String value) {
        if (!StringUtils.hasText(value)) {
            return "<empty>";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= LOG_PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, LOG_PREVIEW_LIMIT) + "...";
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
