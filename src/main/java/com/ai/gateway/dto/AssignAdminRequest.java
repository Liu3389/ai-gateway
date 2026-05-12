package com.ai.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 分配管理员角色请求
 *
 * @author AI Gateway Platform
 */
@Data
public class AssignAdminRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private Long userId;

    /**
     * 角色：ADMIN-管理员，SUPER_ADMIN-超级管理员
     */
    @NotBlank(message = "角色不能为空")
    private String role;
}
