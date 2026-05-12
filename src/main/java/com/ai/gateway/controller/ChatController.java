package com.ai.gateway.controller;

import com.ai.gateway.common.Result;
import com.ai.gateway.dto.ChatRequest;
import com.ai.gateway.service.BillingService;
import com.ai.gateway.service.OpenAiChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天控制器 - 提供流式和非流式对话接口
 * <p>
 * 本控制器是AI网关平台的核心API入口，提供与OpenAI兼容的对话接口。
 * 支持两种响应模式：
 * 1. 非流式响应：一次性返回完整结果
 * 2. 流式响应（SSE）：实时逐字返回生成内容
 * </p>
 *
 * <p><b>API兼容性：</b></p>
 * <p>
 * 本API设计与OpenAI API保持兼容，客户端可以使用相同的代码调用本服务和OpenAI服务。
 * 例如：
 * <pre>{@code
 * // OpenAI官方API
 * POST https://api.openai.com/v1/chat/completions
 *
 * // 本系统API
 * POST http://localhost:8080/api/chat/completions
 * }</pre>
 * </p>
 *
 * <p><b>认证机制：</b></p>
 * <ul>
 *   <li>通过X-API-Key Header进行身份认证</li>
 *   <li>ApiKeyAuthInterceptor拦截器验证API Key有效性</li>
 *   <li>拦截器还会检查限流、余额等</li>
 *   <li>验证通过后，将userId、apiKey、requestId存入HttpServletRequest属性</li>
 * </ul>
 *
 * <p><b>请求流程：</b></p>
 * <ol>
 *   <li>客户端发送POST请求到/chat/completions或/chat/stream</li>
 *   <li>ApiKeyAuthInterceptor拦截请求，验证API Key</li>
 *   <li>验证通过后，将用户信息存入request属性</li>
 *   <li>Controller从request属性获取用户信息</li>
 *   <li>调用OpenAiChatService处理对话逻辑</li>
 *   <li>返回结果给客户端</li>
 * </ol>
 *
 * @author AI Gateway Platform
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final OpenAiChatService openAiChatService;  // OpenAI对话服务，处理实际的AI对话逻辑
    private final BillingService billingService;        // 计费服务，用于查询余额等信息

    /**
     * 非流式对话接口
     * <p>
     * 本接口实现了一次性返回完整响应的对话功能。
     * 客户端发送请求后，等待服务器处理完成，然后一次性返回完整的AI响应。
     * </p>
     *
     * <p><b>适用场景：</b></p>
     * <ul>
     *   <li>批量处理任务</li>
     *   <li>后台自动化任务</li>
     *   <li>不需要实时显示生成过程的场景</li>
     * </ul>
     *
     * <p><b>请求示例：</b></p>
     * <pre>{@code
     * POST /api/chat/completions
     * Headers:
     *   X-API-Key: sk-xxx
     *   Content-Type: application/json
     *
     * Body:
     * {
     *   "model": "gpt-3.5-turbo",
     *   "messages": [
     *     {"role": "user", "content": "你好"}
     *   ]
     * }
     *
     * Response:
     * {
     *   "code": 200,
     *   "message": "操作成功",
     *   "data": "你好！有什么我可以帮助你的吗？",
     *   "timestamp": 1234567890
     * }
     * }</pre>
     *
     * <p><b>错误处理：</b></p>
     * <ul>
     *   <li>API Key无效：由拦截器返回401错误</li>
     *   <li>余额不足：由拦截器返回402错误</li>
     *   <li>限流：由拦截器返回429错误</li>
     *   <li>模型不存在：由Service层返回400错误</li>
     *   <li>OpenAI API调用失败：由全局异常处理器返回500错误</li>
     * </ul>
     *
     * @param request     聊天请求体，包含模型、消息、参数等（通过@Valid自动校验）
     * @param httpRequest HTTP请求对象，从拦截器注入的属性中获取用户信息
     * @return 统一响应对象，data字段包含AI生成的完整文本
     * @throws RuntimeException 当对话失败时抛出，由全局异常处理器处理
     */
    @PostMapping("/completions")
    public Result<String> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        // ============================================
        // 从拦截器获取用户信息
        // ============================================
        // ApiKeyAuthInterceptor在验证通过后，会将以下信息存入request属性：
        // - userId: 用户ID，用于计费和日志记录
        // - apiKey: API Key字符串，用于日志记录
        // - requestId: 请求ID，唯一标识一次请求，用于幂等性和结算
        Long userId = (Long) httpRequest.getAttribute("userId");
        String apiKey = (String) httpRequest.getAttribute("apiKey");
        String requestId = (String) httpRequest.getAttribute("requestId");

        try {
            // 调用OpenAI对话服务，执行实际的AI对话逻辑
            // 该方法会：
            // 1. 验证模型配置
            // 2. 调用OpenAI API
            // 3. 解析响应
            // 4. 计算费用并结算余额
            // 5. 保存调用日志
            String response = openAiChatService.chat(apiKey, userId, request, requestId);

            // 返回成功响应，包含AI生成的文本
            return Result.success(response);
            
        } catch (Exception e) {
            // 记录错误日志，包含requestId便于追踪问题
            log.error("对话失败: requestId={}", requestId, e);
            // 抛出异常，由全局异常处理器（GlobalExceptionHandler）统一处理
            throw e;
        }
    }

    /**
     * 流式对话接口（SSE - Server-Sent Events）
     * <p>
     * 本接口实现了实时流式响应功能，使用SSE技术逐字返回AI生成的内容。
     * 客户端可以在AI生成内容的同时实时接收和显示，提供更好的用户体验。
     * </p>
     *
     * <p><b>什么是SSE？</b></p>
     * <p>
     * SSE（Server-Sent Events）是一种服务器推送技术，允许服务器向客户端持续发送数据。
     * 与WebSocket不同，SSE是单向通信（服务器→客户端），基于HTTP协议，实现简单。
     * </p>
     *
     * <p><b>适用场景：</b></p>
     * <ul>
     *   <li>聊天机器人界面，实时显示生成的文字</li>
     *   <li>需要减少用户等待时间的场景</li>
     *   <li>长文本生成，提升用户体验</li>
     * </ul>
     *
     * <p><b>请求示例：</b></p>
     * <pre>{@code
     * POST /api/chat/stream
     * Headers:
     *   X-API-Key: sk-xxx
     *   Content-Type: application/json
     *   Accept: text/event-stream
     *
     * Body:
     * {
     *   "model": "gpt-3.5-turbo",
     *   "messages": [
     *     {"role": "user", "content": "请写一首诗"}
     *   ],
     *   "stream": true
     * }
     *
     * Response (SSE stream):
     * event: message
     * data: {"content":"春"}
     *
     * event: message
     * data: {"content":"眠"}
     *
     * event: message
     * data: {"content":"不"}
     *
     * event: done
     * data: {"finished":true,"inputTokens":10,"outputTokens":20,"cost":0.00006}
     * }</pre>
     *
     * <p><b>SSE事件类型：</b></p>
     * <ul>
     *   <li><b>message:</b> 携带生成的文本片段，可能多次发送</li>
     *   <li><b>done:</b> 表示生成完成，包含Token数和费用信息</li>
     *   <li><b>error:</b> 发生错误时发送，包含错误信息</li>
     * </ul>
     *
     * <p><b>超时设置：</b></p>
     * <p>
     * SseEmitter默认超时时间为60秒。
     * 如果AI生成时间超过60秒，连接会自动关闭。
     * 对于长文本生成，可能需要调整此值。
     * </p>
     *
     * @param request     聊天请求体，包含模型、消息、参数等（通过@Valid自动校验）
     * @param httpRequest HTTP请求对象，从拦截器注入的属性中获取用户信息
     * @return SseEmitter对象，用于向客户端推送流式数据
     */
    @PostMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        // ============================================
        // 从拦截器获取用户信息
        // ============================================
        // 与非流式接口相同，从request属性中获取用户信息
        Long userId = (Long) httpRequest.getAttribute("userId");
        String apiKey = (String) httpRequest.getAttribute("apiKey");
        String requestId = (String) httpRequest.getAttribute("requestId");

        // 记录流式对话开始日志，便于追踪和调试
        log.info("开始流式对话: userId={}, requestId={}, model={}", userId, requestId, request.getModel());

        // 调用流式对话服务
        // 该方法会：
        // 1. 创建SseEmitter对象
        // 2. 异步调用OpenAI Stream API
        // 3. 实时解析并推送数据块
        // 4. 完成后发送done事件
        // 5. 结算余额并保存日志
        return openAiChatService.streamChat(apiKey, userId, request, requestId);
    }
}
