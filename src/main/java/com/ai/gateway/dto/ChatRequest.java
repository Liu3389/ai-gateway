package com.ai.gateway.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 聊天请求DTO
 * 
 * @author AI Gateway Platform
 */
@Data
public class ChatRequest {

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    private String model;

    /**
     * 消息列表
     */
    @NotNull(message = "消息列表不能为空")
    @NotEmpty(message = "消息列表不能为空")
    private List<ChatMessage> messages;

    /**
     * 是否使用流式响应
     */
    private Boolean stream = false;

    /**
     * 最大Token数
     */
    private Integer maxTokens;

    /**
     * 温度参数（0-2）
     */
    private Double temperature;

    /**
     * 内部消息类
     */
    @Data
    public static class ChatMessage {
        /**
         * 角色：system/user/assistant
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;
    }
}
