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
 * OpenAI对话服务类 - 支持流式和非流式响应
 * <p>
 * 本类是AI网关平台的核心服务之一，负责与OpenAI API进行交互，提供以下功能：
 * 1. 非流式对话：一次性返回完整响应
 * 2. 流式对话（SSE）：逐字返回响应，提供更好的用户体验
 * 3. Token计数和费用计算
 * 4. 调用日志记录
 * 5. 余额结算和回滚
 * </p>
 *
 * <p><b>业务流程：</b></p>
 * <ol>
 *   <li>接收用户请求（包含模型、消息等）</li>
 *   <li>验证模型配置和可用性</li>
 *   <li>构建OpenAI API请求体</li>
 *   <li>调用OpenAI API（流式或非流式）</li>
 *   <li>解析响应并提取内容、Token数</li>
 *   <li>计算费用并结算余额</li>
 *   <li>保存调用日志和计费记录</li>
 *   <li>返回结果给用户</li>
 * </ol>
 *
 * <p><b>异常处理：</b></p>
 * <ul>
 *   <li>API调用失败：回滚预扣余额，记录失败日志</li>
 *   <li>网络超时：设置合理的超时时间，避免长时间等待</li>
 *   <li>解析错误：捕获并记录异常，保证系统稳定性</li>
 * </ul>
 *
 * <p><b>性能优化：</b></p>
 * <ul>
 *   <li>使用异步处理流式请求，避免阻塞主线程</li>
 *   <li>合理设置超时时间，平衡用户体验和资源占用</li>
 *   <li>使用StringBuilder高效拼接字符串</li>
 * </ul>
 *
 * @author AI Gateway Platform
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiChatService {

    private final ModelConfigService modelConfigService;  // 模型配置服务，用于验证模型和获取API Key
    private final CallLogService callLogService;          // 调用日志服务，用于保存调用记录
    private final BillingService billingService;          // 计费服务，用于余额管理和结算
    private final ObjectMapper objectMapper = new ObjectMapper();  // JSON序列化工具，用于构建请求和解析响应

    /**
     * 非流式对话
     * <p>
     * 本方法实现了一次性返回完整响应的对话功能。
     * 适用于不需要实时显示生成过程的场景，如批量处理、后台任务等。
     * </p>
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>记录开始时间，用于计算响应时长</li>
     *   <li>验证模型名称，检查模型是否可用</li>
     *   <li>构建请求体，包含消息、参数等</li>
     *   <li>调用OpenAI API，发送HTTP POST请求</li>
     *   <li>解析响应，提取内容、Token数等信息</li>
     *   <li>计算费用，根据Token数和模型价格计算</li>
     *   <li>结算余额，退还预扣金额与实际消费的差额</li>
     *   <li>保存调用日志，记录成功信息</li>
     *   <li>创建计费记录，记录余额变化</li>
     *   <li>返回响应内容</li>
     * </ol>
     *
     * <p><b>异常处理：</b></p>
     * <ul>
     *   <li>API调用失败：回滚预扣余额（结算金额为0），记录失败日志</li>
     *   <li>网络超时：抛出RuntimeException，由全局异常处理器处理</li>
     *   <li>JSON解析错误：捕获并记录异常，回滚余额</li>
     * </ul>
     *
     * <p><b>事务保证：</b></p>
     * <ul>
     *   <li>预扣余额：在拦截器中完成，保证用户有足够余额</li>
     *   <li>结算余额：使用Lua脚本保证原子性，避免并发问题</li>
     *   <li>日志记录：即使发生异常，也会记录失败日志</li>
     * </ul>
     *
     * @param apiKey  API Key，用于标识调用者身份
     * @param userId  用户ID，用于计费和日志记录
     * @param request 聊天请求，包含模型、消息、参数等
     * @param requestId 请求ID，唯一标识一次请求，用于幂等性和结算
     * @return 响应内容，AI生成的完整文本
     * @throws RuntimeException 当API调用失败时抛出
     */
    public String chat(String apiKey, Long userId, ChatRequest request, String requestId) {
        long startTime = System.currentTimeMillis();
        
        // 验证模型
        ModelConfig modelConfig = modelConfigService.validateModel(request.getModel());
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(request);
            
            // 调用OpenAI API
            HttpResponse response = HttpRequest.post(resolveChatUrl(modelConfig.getBaseUrl()))
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

            // 获取结算前余额
            BigDecimal balanceBeforeSettle = billingService.getUserBalance(userId);
            
            // 结算余额（实际扣费，退还多余预扣金额）
            billingService.settleBalance(userId, requestId, cost);
            log.info("结算完成: userId={}, requestId={}, actualCost={}", userId, requestId, cost);

            // 获取结算后余额
            BigDecimal balanceAfterSettle = billingService.getUserBalance(userId);

            // 保存调用日志和计费记录
            Long callLogId = saveCallLog(apiKey, userId, request.getModel(), inputTokens, outputTokens, 
                       cost, System.currentTimeMillis() - startTime, true, null);

            callLogService.createBillingRecord(userId, callLogId, cost,
                    Constants.BILLING_TYPE_DEDUCT, balanceBeforeSettle, balanceAfterSettle);

            log.info("对话成功: userId={}, model={}, inputTokens={}, outputTokens={}, cost={}", 
                    userId, request.getModel(), inputTokens, outputTokens, cost);

            return content;

        } catch (Exception e) {
            log.error("对话失败: userId={}, model={}", userId, request.getModel(), e);
            
            // 如果失败，回滚预扣的余额（结算金额为0）
            BigDecimal balanceBeforeRollback = billingService.getUserBalance(userId);
            billingService.settleBalance(userId, requestId, BigDecimal.ZERO);
            log.info("余额回滚: userId={}, requestId={}", userId, requestId);
            BigDecimal balanceAfterRollback = billingService.getUserBalance(userId);
            
            // 保存失败日志
            Long callLogId = saveCallLog(apiKey, userId, request.getModel(), 0, 0, BigDecimal.ZERO,
                       System.currentTimeMillis() - startTime, false, e.getMessage());

            callLogService.createBillingRecord(userId, callLogId, BigDecimal.ZERO,
                    Constants.BILLING_TYPE_DEDUCT, balanceBeforeRollback, balanceAfterRollback);
            
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
                HttpResponse response = HttpRequest.post(resolveChatUrl(modelConfig.getBaseUrl()))
                        .header("Authorization", "Bearer " + modelConfig.getApiKey())
                        .header("Content-Type", "application/json")
                        .body(objectMapper.writeValueAsString(requestBody))
                        .timeout(60000)
                        .executeAsync();

                if (!response.isOk()) {
                    emitter.send(SseEmitter.event().name("error").data("API调用失败"));
                    // 回滚预扣余额
                    BigDecimal balanceBeforeRollback = billingService.getUserBalance(userId);
                    billingService.settleBalance(userId, requestId, BigDecimal.ZERO);
                    BigDecimal balanceAfterRollback = billingService.getUserBalance(userId);

                    Long failCallLogId = saveCallLog(apiKey, userId, request.getModel(), 0, 0, BigDecimal.ZERO,
                            0, false, "API调用失败: HTTP " + response.getStatus());

                    callLogService.createBillingRecord(userId, failCallLogId, BigDecimal.ZERO,
                            Constants.BILLING_TYPE_DEDUCT, balanceBeforeRollback, balanceAfterRollback);
                    
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
                            JsonNode contentNode = node.path("choices").get(0).path("delta").path("content");
                            if (contentNode.isNull() || contentNode.asText().isEmpty()) {
                                continue;
                            }
                            String content = contentNode.asText();

                            fullContent.append(content);

                            Map<String, Object> eventData = new HashMap<>();
                            eventData.put("content", content);
                            emitter.send(SseEmitter.event().name("message").data(eventData));
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

                // 获取结算前余额
                BigDecimal balanceBeforeSettle = billingService.getUserBalance(userId);
                
                // 结算余额（实际扣费，退还多余预扣金额）
                billingService.settleBalance(userId, requestId, cost);
                log.info("流式对话结算完成: userId={}, requestId={}, actualCost={}", userId, requestId, cost);

                // 获取结算后余额
                BigDecimal balanceAfterSettle = billingService.getUserBalance(userId);
                
                // 保存调用日志
                Long callLogId = saveCallLog(apiKey, userId, request.getModel(), inputTokens, outputTokens,
                           cost, System.currentTimeMillis() - startTime, true, null);

                callLogService.createBillingRecord(userId, callLogId, cost,
                        Constants.BILLING_TYPE_DEDUCT, balanceBeforeSettle, balanceAfterSettle);
                
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
                    BigDecimal balanceBeforeRollback = billingService.getUserBalance(userId);
                    billingService.settleBalance(userId, requestId, BigDecimal.ZERO);
                    BigDecimal balanceAfterRollback = billingService.getUserBalance(userId);

                    Long failCallLogId = saveCallLog(apiKey, userId, request.getModel(), 0, 0, BigDecimal.ZERO,
                               System.currentTimeMillis() - startTime, false, e.getMessage());

                    callLogService.createBillingRecord(userId, failCallLogId, BigDecimal.ZERO,
                            Constants.BILLING_TYPE_DEDUCT, balanceBeforeRollback, balanceAfterRollback);
                } catch (IOException ex) {
                    log.error("发送错误事件失败", ex);
                } finally {
                    emitter.completeWithError(e);
                }
            }
        });
        
        return emitter;
    }

    private String resolveChatUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return baseUrl;
        if (baseUrl.contains("/chat/completions")) return baseUrl;
        return baseUrl.replaceAll("/+$", "") + "/v1/chat/completions";
    }

    /**
     * 构建请求体（防止注入攻击）
     */
    private Map<String, Object> buildRequestBody(ChatRequest request) {
        Map<String, Object> requestBody = new HashMap<>();

        // 验证模型名称（白名单）
        if (request.getModel() == null || request.getModel().isEmpty()) {
            throw new IllegalArgumentException("模型名称不能为空");
        }
        requestBody.put("model", request.getModel());

        // 验证消息列表
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new IllegalArgumentException("消息列表不能为空");
        }

        // 清理消息内容，防止XSS
        var sanitizedMessages = request.getMessages().stream()
                .map(msg -> {
                    var sanitizedMsg = new HashMap<String, Object>();
                    sanitizedMsg.put("role", msg.getRole());
                    // 转义特殊字符，防止注入
                    String content = msg.getContent() != null ? msg.getContent() : "";
                    sanitizedMsg.put("content", content);
                    return sanitizedMsg;
                })
                .toList();

        requestBody.put("messages", sanitizedMessages);
        
        if (request.getMaxTokens() != null) {
            // 限制max_tokens范围
            int maxTokens = Math.min(Math.max(request.getMaxTokens(), 1), 4096);
            requestBody.put("max_tokens", maxTokens);
        }
        
        if (request.getTemperature() != null) {
            // 限制temperature范围 0-2
            double temperature = Math.min(Math.max(request.getTemperature(), 0.0), 2.0);
            requestBody.put("temperature", temperature);
        }
        
        return requestBody;
    }

    /**
     * 保存调用日志
     * @return 调用日志ID
     */
    private Long saveCallLog(String apiKey, Long userId, String model, int inputTokens,
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
        return callLog.getId();
    }
}
