package com.ai.gateway.controller;

import com.ai.gateway.common.Result;
import com.ai.gateway.dto.ApiKeyGenerateRequest;
import com.ai.gateway.service.ApiKeyService;
import com.ai.gateway.vo.ApiKeyInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API Key管理控制器
 * 
 * @author AI Gateway Platform
 */
@RestController
@RequestMapping("/api-key")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * 生成API Key
     */
    @PostMapping("/generate")
    public Result<ApiKeyInfoVO> generateApiKey(
            @RequestParam Long userId,
            @RequestBody(required = false) ApiKeyGenerateRequest request) {
        
        String name = request != null ? request.getName() : null;
        Integer rateLimit = request != null ? request.getRateLimit() : null;
        
        ApiKeyInfoVO apiKeyInfo = apiKeyService.generateApiKey(userId, name, rateLimit);
        return Result.success("API Key生成成功", apiKeyInfo);
    }

    /**
     * 删除API Key
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteApiKey(@PathVariable Long id, @RequestParam Long userId) {
        apiKeyService.deleteApiKey(id, userId);
        return Result.success("删除成功", null);
    }

    /**
     * 启用API Key
     */
    @PutMapping("/{id}/enable")
    public Result<Void> enableApiKey(@PathVariable Long id, @RequestParam Long userId) {
        apiKeyService.enableApiKey(id, userId);
        return Result.success("启用成功", null);
    }

    /**
     * 禁用API Key
     */
    @PutMapping("/{id}/disable")
    public Result<Void> disableApiKey(@PathVariable Long id, @RequestParam Long userId) {
        apiKeyService.disableApiKey(id, userId);
        return Result.success("禁用成功", null);
    }

    /**
     * 更新限流阈值
     */
    @PutMapping("/{id}/rate-limit")
    public Result<Void> updateRateLimit(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam Integer rateLimit) {
        apiKeyService.updateRateLimit(id, userId, rateLimit);
        return Result.success("更新成功", null);
    }

    /**
     * 查询用户的API Key列表
     */
    @GetMapping("/list")
    public Result<List<ApiKeyInfoVO>> listApiKeys(@RequestParam Long userId) {
        List<ApiKeyInfoVO> apiKeys = apiKeyService.listApiKeys(userId);
        return Result.success(apiKeys);
    }
}
