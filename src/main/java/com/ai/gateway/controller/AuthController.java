package com.ai.gateway.controller;

import com.ai.gateway.common.Result;
import com.ai.gateway.dto.UserLoginRequest;
import com.ai.gateway.dto.UserRegisterRequest;
import com.ai.gateway.service.UserService;
import com.ai.gateway.vo.LoginResponseVO;
import com.ai.gateway.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 用户控制器
 * 
 * @author AI Gateway Platform
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserInfoVO> register(@Valid @RequestBody UserRegisterRequest request) {
        UserInfoVO userInfo = userService.register(request);
        return Result.success("注册成功", userInfo);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponseVO> login(@Valid @RequestBody UserLoginRequest request) {
        LoginResponseVO response = userService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 查询用户信息
     */
    @GetMapping("/userinfo")
    public Result<UserInfoVO> getUserInfo(@RequestParam Long userId) {
        UserInfoVO userInfo = userService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    /**
     * 查询余额
     */
    @GetMapping("/balance")
    public Result<BigDecimal> getBalance(@RequestParam Long userId) {
        BigDecimal balance = userService.getBalance(userId);
        return Result.success(balance);
    }
}
