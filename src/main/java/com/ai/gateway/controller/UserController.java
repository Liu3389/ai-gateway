package com.ai.gateway.controller;

import com.ai.gateway.common.Result;
import com.ai.gateway.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/recharge")
    public Result<Void> recharge(@RequestParam BigDecimal amount, HttpServletRequest request) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("充值金额必须大于0");
        }
        Long userId = (Long) request.getAttribute("currentUserId");
        userService.recharge(userId, amount);
        return Result.success("充值成功", null);
    }

    @GetMapping("/info")
    public Result<?> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(userService.getUserInfo(userId));
    }

    @GetMapping("/balance")
    public Result<BigDecimal> getBalance(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(userService.getBalance(userId));
    }
}
