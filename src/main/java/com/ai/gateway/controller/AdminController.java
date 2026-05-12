package com.ai.gateway.controller;

import com.ai.gateway.common.Result;
import com.ai.gateway.dto.AssignAdminRequest;
import com.ai.gateway.dto.SetFreeApiStrategyRequest;
import com.ai.gateway.service.AdminService;
import com.ai.gateway.vo.AdminInfoVO;
import com.ai.gateway.vo.SystemStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员控制器
 *
 * @author AI Gateway Platform
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 分配管理员角色（仅超级管理员）
     */
    @PostMapping("/assign-role")
    public Result<Void> assignAdminRole(
            @RequestHeader("X-User-Id") Long operatorId,
            @Valid @RequestBody AssignAdminRequest request) {
        adminService.assignAdminRole(operatorId, request);
        return Result.success("角色分配成功", null);
    }

    /**
     * 取消管理员角色（仅超级管理员）
     */
    @PostMapping("/revoke-role")
    public Result<Void> revokeAdminRole(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestParam Long userId) {
        adminService.revokeAdminRole(operatorId, userId);
        return Result.success("角色取消成功", null);
    }

    /**
     * 设置API免费策略（仅超级管理员）
     */
    @PostMapping("/set-free-strategy")
    public Result<Void> setFreeApiStrategy(
            @RequestHeader("X-User-Id") Long operatorId,
            @Valid @RequestBody SetFreeApiStrategyRequest request) {
        adminService.setFreeApiStrategy(operatorId, request);
        return Result.success("免费策略设置成功", null);
    }

    /**
     * 获取所有管理员列表（仅超级管理员）
     */
    @GetMapping("/admins")
    public Result<List<AdminInfoVO>> getAllAdmins(@RequestHeader("X-User-Id") Long operatorId) {
        List<AdminInfoVO> admins = adminService.getAllAdmins(operatorId);
        return Result.success(admins);
    }

    /**
     * 获取所有用户列表（管理员及以上）
     */
    @GetMapping("/users")
    public Result<List<AdminInfoVO>> getAllUsers(@RequestHeader("X-User-Id") Long operatorId) {
        List<AdminInfoVO> users = adminService.getAllUsers(operatorId);
        return Result.success(users);
    }

    /**
     * 获取用户详细信息（管理员及以上）
     */
    @GetMapping("/user/{userId}")
    public Result<AdminInfoVO> getUserDetail(
            @RequestHeader("X-User-Id") Long operatorId,
            @PathVariable Long userId) {
        AdminInfoVO userDetail = adminService.getUserDetail(operatorId, userId);
        return Result.success(userDetail);
    }

    /**
     * 禁用/启用用户（管理员及以上）
     */
    @PostMapping("/user/status")
    public Result<Void> updateUserStatus(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestParam Long userId,
            @RequestParam Integer status) {
        adminService.updateUserStatus(operatorId, userId, status);
        return Result.success(status == 1 ? "用户已启用" : "用户已禁用", null);
    }

    /**
     * 获取系统统计信息（管理员及以上）
     */
    @GetMapping("/stats")
    public Result<SystemStatsVO> getSystemStats(@RequestHeader("X-User-Id") Long operatorId) {
        SystemStatsVO stats = adminService.getSystemStats(operatorId);
        return Result.success(stats);
    }
}
