package com.ai.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设置API免费策略请求
 *
 * @author AI Gateway Platform
 */
@Data
public class SetFreeApiStrategyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * API免费策略：UNLIMITED-完全免费，QUOTA_BASED-额度免费，COUNT_LIMITED-限次免费等
     */
    @NotBlank(message = "免费策略不能为空")
    private String freeApiStrategy;

    /**
     * 免费额度（美元），仅当freeApiStrategy为QUOTA_BASED时有效
     */
    private BigDecimal freeQuota;

    /**
     * 每日调用次数限制，仅当freeApiStrategy为COUNT_LIMITED时有效
     */
    private Integer dailyCallLimit;

    /**
     * 每月调用次数限制，仅当freeApiStrategy为COUNT_LIMITED时有效
     */
    private Integer monthlyCallLimit;

    /**
     * 免费策略开始时间
     */
    private LocalDateTime freeStrategyStartTime;

    /**
     * 免费策略结束时间
     */
    private LocalDateTime freeStrategyEndTime;

    /**
     * 允许免费的模型列表（JSON格式），仅当freeApiStrategy为MODEL_SPECIFIC时有效
     */
    private String allowedFreeModels;
}
