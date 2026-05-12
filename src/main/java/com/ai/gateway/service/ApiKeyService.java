package com.ai.gateway.service;

import cn.hutool.core.util.IdUtil;
import com.ai.gateway.common.Constants;
import com.ai.gateway.common.ResultCode;
import com.ai.gateway.entity.ApiKey;
import com.ai.gateway.exception.BusinessException;
import com.ai.gateway.mapper.ApiKeyMapper;
import com.ai.gateway.vo.ApiKeyInfoVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API Key服务类
 * 
 * @author AI Gateway Platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyMapper apiKeyMapper;

    /**
     * 生成API Key
     * 
     * @param userId 用户ID
     * @param name API Key名称
     * @param rateLimit 限流阈值
     * @return API Key信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyInfoVO generateApiKey(Long userId, String name, Integer rateLimit) {
        // 生成唯一的API Key
        String apiKey = "sk-" + IdUtil.simpleUUID();

        ApiKey entity = new ApiKey();
        entity.setApiKey(apiKey);
        entity.setUserId(userId);
        entity.setName(name);
        entity.setStatus(Constants.API_KEY_STATUS_ENABLED);
        entity.setRateLimit(rateLimit != null ? rateLimit : Constants.DEFAULT_RATE_LIMIT);
        entity.setCreateTime(LocalDateTime.now());

        apiKeyMapper.insert(entity);

        log.info("API Key生成成功: userId={}, apiKey={}", userId, apiKey);

        return convertToVO(entity);
    }

    /**
     * 删除API Key
     * 
     * @param apiKeyId API Key ID
     * @param userId 用户ID（用于权限校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteApiKey(Long apiKeyId, Long userId) {
        ApiKey apiKey = apiKeyMapper.selectById(apiKeyId);
        if (apiKey == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "API Key不存在");
        }

        // 权限校验
        if (!apiKey.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作此API Key");
        }

        apiKeyMapper.deleteById(apiKeyId);

        log.info("API Key删除成功: apiKeyId={}, userId={}", apiKeyId, userId);
    }

    /**
     * 启用API Key
     * 
     * @param apiKeyId API Key ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void enableApiKey(Long apiKeyId, Long userId) {
        updateApiKeyStatus(apiKeyId, userId, Constants.API_KEY_STATUS_ENABLED);
    }

    /**
     * 禁用API Key
     * 
     * @param apiKeyId API Key ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableApiKey(Long apiKeyId, Long userId) {
        updateApiKeyStatus(apiKeyId, userId, Constants.API_KEY_STATUS_DISABLED);
    }

    /**
     * 更新API Key限流阈值
     * 
     * @param apiKeyId API Key ID
     * @param userId 用户ID
     * @param rateLimit 新的限流阈值
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRateLimit(Long apiKeyId, Long userId, Integer rateLimit) {
        ApiKey apiKey = apiKeyMapper.selectById(apiKeyId);
        if (apiKey == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "API Key不存在");
        }

        if (!apiKey.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作此API Key");
        }

        apiKey.setRateLimit(rateLimit);
        apiKeyMapper.updateById(apiKey);

        log.info("API Key限流阈值更新成功: apiKeyId={}, rateLimit={}", apiKeyId, rateLimit);
    }

    /**
     * 查询用户的API Key列表
     * 
     * @param userId 用户ID
     * @return API Key列表
     */
    public List<ApiKeyInfoVO> listApiKeys(Long userId) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getUserId, userId)
               .orderByDesc(ApiKey::getCreateTime);

        List<ApiKey> apiKeys = apiKeyMapper.selectList(wrapper);
        return apiKeys.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据API Key字符串查询API Key信息
     * 
     * @param apiKey API Key字符串
     * @return API Key实体
     */
    public ApiKey getByApiKey(String apiKey) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getApiKey, apiKey);
        return apiKeyMapper.selectOne(wrapper);
    }

    /**
     * 验证API Key是否有效
     * 
     * @param apiKey API Key字符串
     * @return API Key实体
     */
    public ApiKey validateApiKey(String apiKey) {
        ApiKey entity = getByApiKey(apiKey);
        
        if (entity == null) {
            throw new BusinessException(ResultCode.INVALID_API_KEY);
        }

        if (entity.getStatus() == Constants.API_KEY_STATUS_DISABLED) {
            throw new BusinessException(ResultCode.API_KEY_DISABLED);
        }

        // 检查是否过期
        if (entity.getExpireTime() != null && entity.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.INVALID_API_KEY.getCode(), "API Key已过期");
        }

        return entity;
    }

    /**
     * 更新API Key状态
     * 
     * @param apiKeyId API Key ID
     * @param userId 用户ID
     * @param status 状态
     */
    private void updateApiKeyStatus(Long apiKeyId, Long userId, Integer status) {
        ApiKey apiKey = apiKeyMapper.selectById(apiKeyId);
        if (apiKey == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "API Key不存在");
        }

        if (!apiKey.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作此API Key");
        }

        apiKey.setStatus(status);
        apiKeyMapper.updateById(apiKey);

        log.info("API Key状态更新成功: apiKeyId={}, status={}", apiKeyId, status);
    }

    /**
     * 转换为VO对象
     * 
     * @param entity API Key实体
     * @return API Key信息VO
     */
    private ApiKeyInfoVO convertToVO(ApiKey entity) {
        ApiKeyInfoVO vo = new ApiKeyInfoVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
