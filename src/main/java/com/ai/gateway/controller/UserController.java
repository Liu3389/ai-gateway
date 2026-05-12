package com.ai.gateway.controller;

import com.ai.gateway.common.Result;
import com.ai.gateway.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 用户管理控制器
 * 
 * @author AI Gateway Platform
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 充值接口
     * 
     * @param userId 用户ID
     * @param amount 充值金额（美元）
     * @return 充值结果
     */
    @PostMapping("/recharge")
    public Result<Void> recharge(@RequestParam Long userId, @RequestParam BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("充值金额必须大于0");
        }
        
        userService.recharge(userId, amount);
        return Result.success("充值成功", null);
    }

    /**
     * 查询用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<?> getUserInfo(@RequestParam Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }

    /**
     * 查询余额
     * 
     * @param userId 用户ID
     * @return 余额
     */
    @GetMapping("/balance")
    public Result<BigDecimal> getBalance(@RequestParam Long userId) {
        return Result.success(userService.getBalance(userId));
    }
}
