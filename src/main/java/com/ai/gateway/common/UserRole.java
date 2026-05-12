package com.ai.gateway.common;

/**
 * 用户角色枚举
 *
 * @author AI Gateway Platform
 */
public enum UserRole {

    /**
     * 超级管理员 - 最高权限，可任命/取消管理员，分配API免费权限
     */
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员"),

    /**
     * 管理员 - 可查看可视化数据，管理普通用户
     */
    ADMIN("ADMIN", "管理员"),

    /**
     * 普通用户 - 只能使用API服务
     */
    USER("USER", "普通用户");

    private final String code;
    private final String description;

    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取角色
     */
    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return USER; // 默认为普通用户
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
