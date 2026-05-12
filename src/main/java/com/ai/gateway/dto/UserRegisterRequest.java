package com.ai.gateway.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户注册请求DTO
 * 
 * @author AI Gateway Platform
 */
@Data
public class UserRegisterRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 邮箱
     */
    private String email;
}
