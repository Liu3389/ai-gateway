package com.ai.gateway.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.ai.gateway.common.Constants;
import com.ai.gateway.dto.ChatRequest;
import com.ai.gateway.entity.CallLog;
import com.ai.gateway.entity.ModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * OpenAI对话服务 - 支持流式和非流式响应
 * 
 * @author AI Gateway Platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiChatService {

    private final ModelConfigService modelConfigService;
    private final CallLogService callLogService;
    private final BillingService billingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 非流式对话
     * 
     * @param apiKey API Key
     * @param userId 用户ID
     * @param request 聊天请求
     * @param requestId 请求ID（从拦截器传入）
     * @return 响应内容
     */
    public String chat(String apiKey, Long userId, ChatRequest request, String requestId) {
        long startTime = System.currentTimeMillis();
        
        // 验证模型
        ModelConfig modelConfig = modelConfigService.validateModel(request.getModel());
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(request);
            
            // 调用OpenAI API
            HttpResponse response = HttpRequest.post(modelConfig.getBaseUrl())
                    .header("Authorization", "Bearer " + modelConfig.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(requestBody))
                    .timeout(30000)
                    .execute();

            if (!response.isOk()) {
                throw new RuntimeException("OpenAI API调用失败: " + response.body());
            }

            // 解析响应
            JsonNode rootNode = objectMapper.readTree(response.body());
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();
            int inputTokens = rootNode.path("usage").path("prompt_tokens").asInt();
            int outputTokens = rootNode.path("usage").path("completion_tokens").asInt();

            // 计算费用
            BigDecimal cost = modelConfigService.calculateCost(modelConfig, inputTokens, outputTokens);
            
            // 结算余额（实际扣费，退还多余预扣金额）
            billingService.settleBalance(userId, requestId, cost);
            log.info("结算完成: userId={}, requestId={}, actualCost={}", userId, requestId, cost);
            
            // 保存调用日志
            saveCallLog(apiKey, userId, request.getModel(), inputTokens, outputTokens, 
                       cost, System.currentTimeMillis() - startTime, true, null);

            log.info("对话成功: userId={}, model={}, inputTokens={}, outputTokens={}, cost={}", 
                    userId, request.getModel(), inputTokens, outputTokens, cost);

            return content;

        } catch (Exception e) {
            log.error("对话失败: userId={}, model={}", userId, request.getModel(), e);
            
            // 如果失败，回滚预扣的余额（结算金额为0）
            billingService.settleBalance(userId, requestId, BigDecimal.ZERO);
            log.info("余额回滚: userId={}, requestId={}", userId, requestId);
            
            // 保存失败日志
            saveCallLog(apiKey, userId, request.getModel(), 0, 0, BigDecimal.ZERO,
                       System.currentTimeMillis() - startTime, false, e.getMessage());
            
            throw new RuntimeException("对话失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式对话（SSE）
     * 
     * @param apiKey API Key
     * @param userId 用户ID
     * @param request 聊天请求
     * @param requestId 请求ID（从拦截器传入）
     * @return SseEmitter
     */
    public SseEmitter streamChat(String apiKey, Long userId, ChatRequest request, String requestId) {
        long startTime = System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        // 验证模型
        ModelConfig modelConfig = modelConfigService.validateModel(request.getModel());
        
        // 异步执行流式请求
        CompletableFuture.runAsync(() -> {
            try {
                // 构建请求体（启用流式）
                Map<String, Object> requestBody = buildRequestBody(request);
                requestBody.put("stream", true);
                
                // 调用OpenAI Stream API
                HttpResponse response = HttpRequest.post(modelConfig.getBaseUrl())
                        .header("Authorization", "Bearer " + modelConfig.getApiKey())
                        .header("Content-Type", "application/json")
                        .body(objectMapper.writeValueAsString(requestBody))
                        .timeout(60000)
                        .executeAsync();

                if (!response.isOk()) {
                    emitter.send(SseEmitter.event().name("error").data("API调用失败"));
                    // 回滚预扣余额
                    billingService.settleBalance(userId, requestId, BigDecimal.ZERO);
                    emitter.complete();
                    return;
                }

                // 读取流式响应
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.bodyStream(), StandardCharsets.UTF_8));
                
                StringBuilder fullContent = new StringBuilder();
                int inputTokens = 0;
                int outputTokens = 0;
                
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        
                        try {
                            JsonNode node = objectMapper.readTree(data);
                            String content = node.path("choices").get(0).path("delta").path("content").asText();
                            
                            if (content != null && !content.isEmpty()) {
                                fullContent.append(content);
                                
                                // 发送数据块到前端
                                Map<String, Object> eventData = new HashMap<>();
                                eventData.put("content", content);
                                emitter.send(SseEmitter.event().name("message").data(eventData));
                            }
                        } catch (Exception e) {
                            log.warn("解析流式数据失败", e);
                        }
                    }
                }
                
                // 估算Token数（简单估算：每4个字符约1个Token）
                String promptText = request.getMessages().stream()
                        .map(msg -> msg.getContent() != null ? msg.getContent() : "")
                        .reduce("", String::concat);
                inputTokens = promptText.length() / 4;
                outputTokens = fullContent.length() / 4;
                
                // 计算费用
                BigDecimal cost = modelConfigService.calculateCost(modelConfig, inputTokens, outputTokens);
                
                // 结算余额（实际扣费，退还多余预扣金额）
                billingService.settleBalance(userId, requestId, cost);
                log.info("流式对话结算完成: userId={}, requestId={}, actualCost={}", userId, requestId, cost);
                
                // 保存调用日志
                saveCallLog(apiKey, userId, request.getModel(), inputTokens, outputTokens,
                           cost, System.currentTimeMillis() - startTime, true, null);
                
                // 发送完成事件
                Map<String, Object> doneData = new HashMap<>();
                doneData.put("finished", true);
                doneData.put("inputTokens", inputTokens);
                doneData.put("outputTokens", outputTokens);
                doneData.put("cost", cost);
                emitter.send(SseEmitter.event().name("done").data(doneData));
                
                emitter.complete();
                log.info("流式对话完成: userId={}, contentLength={}", userId, fullContent.length());

            } catch (Exception e) {
                log.error("流式对话失败: userId={}", userId, e);
                
                try {
                    emitter.send(SseEmitter.event().name("error").data("对话失败: " + e.getMessage()));
                    
                    // 回滚预扣余额
                    billingService.settleBalance(userId, requestId, BigDecimal.ZERO);
                    
                    saveCallLog(apiKey, userId, request.getModel(), 0, 0, BigDecimal.ZERO,
                               System.currentTimeMillis() - startTime, false, e.getMessage());
                } catch (IOException ex) {
                    log.error("发送错误事件失败", ex);
                } finally {
                    emitter.completeWithError(e);
                }
            }
        });
        
        return emitter;
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(ChatRequest request) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", request.getModel());
        requestBody.put("messages", request.getMessages());
        
        if (request.getMaxTokens() != null) {
            requestBody.put("max_tokens", request.getMaxTokens());
        }
        
        if (request.getTemperature() != null) {
            requestBody.put("temperature", request.getTemperature());
        }
        
        return requestBody;
    }

    /**
     * 保存调用日志
     */
    private void saveCallLog(String apiKey, Long userId, String model, int inputTokens,
                             int outputTokens, BigDecimal cost, long duration, 
                             boolean success, String errorMessage) {
        CallLog callLog = new CallLog();
        callLog.setApiKey(apiKey);
        callLog.setUserId(userId);
        callLog.setModel(model);
        callLog.setInputTokens(inputTokens);
        callLog.setOutputTokens(outputTokens);
        callLog.setCost(cost);
        callLog.setDuration((int) duration);
        callLog.setStatus(success ? Constants.CALL_STATUS_SUCCESS : Constants.CALL_STATUS_FAILED);
        callLog.setErrorMessage(errorMessage);
        
        callLogService.saveCallLog(callLog);
    }
}
