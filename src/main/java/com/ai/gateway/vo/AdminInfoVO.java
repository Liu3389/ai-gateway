package com.ai.gateway.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员信息VO
 *
 * @author AI Gateway Platform
 */
@Data
public class AdminInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 账户余额（美元）
     */
    private BigDecimal balance;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 用户角色：USER-普通用户，ADMIN-管理员，SUPER_ADMIN-超级管理员
     */
    private String role;

    /**
     * API免费策略
     */
    private String freeApiStrategy;

    /**
     * 免费额度（美元）
     */
    private BigDecimal freeQuota;

    /**
     * 每日调用次数限制
     */
    private Integer dailyCallLimit;

    /**
     * 每月调用次数限制
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
     * 允许免费的模型列表（JSON格式）
     */
    private String allowedFreeModels;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
