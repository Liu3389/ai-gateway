package com.ai.gateway.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户信息VO
 * 
 * @author AI Gateway Platform
 */
@Data
public class UserInfoVO {

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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
