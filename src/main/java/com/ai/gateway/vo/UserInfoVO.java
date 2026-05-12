package com.ai.gateway.vo;

import lombok.Data;

import java.math.BigDecimal;

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
}
