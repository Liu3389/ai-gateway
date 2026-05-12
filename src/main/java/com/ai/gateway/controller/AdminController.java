package com.ai.gateway.controller;

import com.ai.gateway.common.Result;
import com.ai.gateway.dto.AssignAdminRequest;
import com.ai.gateway.dto.SetFreeApiStrategyRequest;
import com.ai.gateway.service.AdminService;
import com.ai.gateway.vo.AdminInfoVO;
import com.ai.gateway.vo.SystemStatsVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    private Long getOperatorId(HttpServletRequest request) {
        return (Long) request.getAttribute("currentUserId");
    }

    @PostMapping("/assign-role")
    public Result<Void> assignAdminRole(
            @Valid @RequestBody AssignAdminRequest request,
            HttpServletRequest httpRequest) {
        adminService.assignAdminRole(getOperatorId(httpRequest), request);
        return Result.success("角色分配成功", null);
    }

    @PostMapping("/revoke-role")
    public Result<Void> revokeAdminRole(
            @RequestParam Long userId,
            HttpServletRequest httpRequest) {
        adminService.revokeAdminRole(getOperatorId(httpRequest), userId);
        return Result.success("角色取消成功", null);
    }

    @PostMapping("/set-free-strategy")
    public Result<Void> setFreeApiStrategy(
            @Valid @RequestBody SetFreeApiStrategyRequest request,
            HttpServletRequest httpRequest) {
        adminService.setFreeApiStrategy(getOperatorId(httpRequest), request);
        return Result.success("免费策略设置成功", null);
    }

    @GetMapping("/admins")
    public Result<List<AdminInfoVO>> getAllAdmins(HttpServletRequest httpRequest) {
        List<AdminInfoVO> admins = adminService.getAllAdmins(getOperatorId(httpRequest));
        return Result.success(admins);
    }

    @GetMapping("/users")
    public Result<List<AdminInfoVO>> getAllUsers(HttpServletRequest httpRequest) {
        List<AdminInfoVO> users = adminService.getAllUsers(getOperatorId(httpRequest));
        return Result.success(users);
    }

    @GetMapping("/user/{userId}")
    public Result<AdminInfoVO> getUserDetail(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        AdminInfoVO userDetail = adminService.getUserDetail(getOperatorId(httpRequest), userId);
        return Result.success(userDetail);
    }

    @PostMapping("/user/status")
    public Result<Void> updateUserStatus(
            @RequestParam Long userId,
            @RequestParam Integer status,
            HttpServletRequest httpRequest) {
        adminService.updateUserStatus(getOperatorId(httpRequest), userId, status);
        return Result.success(status == 1 ? "用户已启用" : "用户已禁用", null);
    }

    @GetMapping("/stats")
    public Result<SystemStatsVO> getSystemStats(HttpServletRequest httpRequest) {
        SystemStatsVO stats = adminService.getSystemStats(getOperatorId(httpRequest));
        return Result.success(stats);
    }
}
