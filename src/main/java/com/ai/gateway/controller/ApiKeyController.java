package com.ai.gateway.controller;

import com.ai.gateway.common.Result;
import com.ai.gateway.dto.ApiKeyGenerateRequest;
import com.ai.gateway.service.ApiKeyService;
import com.ai.gateway.vo.ApiKeyInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api-key")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping("/generate")
    public Result<ApiKeyInfoVO> generateApiKey(
            @RequestBody(required = false) ApiKeyGenerateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("currentUserId");
        String name = request != null ? request.getName() : null;
        Integer rateLimit = request != null ? request.getRateLimit() : null;
        ApiKeyInfoVO apiKeyInfo = apiKeyService.generateApiKey(userId, name, rateLimit);
        return Result.success("API Key生成成功", apiKeyInfo);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteApiKey(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("currentUserId");
        apiKeyService.deleteApiKey(id, userId);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/enable")
    public Result<Void> enableApiKey(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("currentUserId");
        apiKeyService.enableApiKey(id, userId);
        return Result.success("启用成功", null);
    }

    @PutMapping("/{id}/disable")
    public Result<Void> disableApiKey(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("currentUserId");
        apiKeyService.disableApiKey(id, userId);
        return Result.success("禁用成功", null);
    }

    @PutMapping("/{id}/rate-limit")
    public Result<Void> updateRateLimit(
            @PathVariable Long id,
            @RequestParam Integer rateLimit,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("currentUserId");
        apiKeyService.updateRateLimit(id, userId, rateLimit);
        return Result.success("更新成功", null);
    }

    @GetMapping("/list")
    public Result<List<ApiKeyInfoVO>> listApiKeys(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("currentUserId");
        List<ApiKeyInfoVO> apiKeys = apiKeyService.listApiKeys(userId);
        return Result.success(apiKeys);
    }
}
