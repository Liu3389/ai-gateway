package com.ai.gateway.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 系统统计信息VO
 *
 * @author AI Gateway Platform
 */
@Data
public class SystemStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总用户数
     */
    private Long totalUsers;

    /**
     * 活跃用户数（最近7天有调用记录）
     */
    private Long activeUsers;

    /**
     * 管理员数量
     */
    private Long adminCount;

    /**
     * 超级管理员数量
     */
    private Long superAdminCount;

    /**
     * 禁用用户数
     */
    private Long disabledUsers;

    /**
     * 总API调用次数
     */
    private Long totalApiCalls;

    /**
     * 今日API调用次数
     */
    private Long todayApiCalls;

    /**
     * 总消费金额（美元）
     */
    private BigDecimal totalRevenue;

    /**
     * 今日消费金额（美元）
     */
    private BigDecimal todayRevenue;

    /**
     * 平均每次调用费用（美元）
     */
    private BigDecimal avgCallCost;

    /**
     * 使用免费策略的用户数
     */
    private Long freeStrategyUsers;

    /**
     * 模型配置数量
     */
    private Long modelCount;

    /**
     * 启用的模型数量
     */
    private Long activeModelCount;
}
