package com.ai.gateway.common;

/**
 * API免费使用策略枚举
 *
 * @author AI Gateway Platform
 */
public enum FreeApiStrategy {

    /**
     * 完全免费 - 不限制调用次数和Token数量
     */
    UNLIMITED("UNLIMITED", "完全免费"),

    /**
     * 额度免费 - 每月赠送固定额度
     */
    QUOTA_BASED("QUOTA_BASED", "额度免费"),

    /**
     * 限次免费 - 每天/每月限制调用次数
     */
    COUNT_LIMITED("COUNT_LIMITED", "限次免费"),

    /**
     * 限时免费 - 在特定时间段内免费
     */
    TIME_LIMITED("TIME_LIMITED", "限时免费"),

    /**
     * 模型限定免费 - 仅对特定模型免费
     */
    MODEL_SPECIFIC("MODEL_SPECIFIC", "模型限定免费");

    private final String code;
    private final String description;

    FreeApiStrategy(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取策略
     */
    public static FreeApiStrategy fromCode(String code) {
        for (FreeApiStrategy strategy : values()) {
            if (strategy.getCode().equals(code)) {
                return strategy;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
