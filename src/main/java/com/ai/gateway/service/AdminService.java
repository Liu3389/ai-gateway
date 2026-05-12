package com.ai.gateway.service;

import com.ai.gateway.common.FreeApiStrategy;
import com.ai.gateway.common.ResultCode;
import com.ai.gateway.common.UserRole;
import com.ai.gateway.dto.AssignAdminRequest;
import com.ai.gateway.dto.SetFreeApiStrategyRequest;
import com.ai.gateway.entity.User;
import com.ai.gateway.exception.BusinessException;
import com.ai.gateway.mapper.CallLogMapper;
import com.ai.gateway.mapper.ModelConfigMapper;
import com.ai.gateway.mapper.UserMapper;
import com.ai.gateway.vo.AdminInfoVO;
import com.ai.gateway.vo.SystemStatsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员服务类
 *
 * @author AI Gateway Platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final CallLogMapper callLogMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final UserService userService;

    /**
     * 验证用户是否为超级管理员
     *
     * @param userId 用户ID
     */
    public void validateSuperAdmin(Long userId) {
        User user = userService.getUserEntityById(userId);
        if (!UserRole.SUPER_ADMIN.getCode().equals(user.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有超级管理员才能执行此操作");
        }
    }

    /**
     * 验证用户是否为管理员或超级管理员
     *
     * @param userId 用户ID
     */
    public void validateAdminOrSuperAdmin(Long userId) {
        User user = userService.getUserEntityById(userId);
        String role = user.getRole();
        if (!UserRole.ADMIN.getCode().equals(role) && !UserRole.SUPER_ADMIN.getCode().equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "需要管理员权限");
        }
    }

    /**
     * 分配管理员角色（仅超级管理员可操作）
     *
     * @param operatorId 操作用户ID（超级管理员）
     * @param request    分配请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignAdminRole(Long operatorId, AssignAdminRequest request) {
        // 验证操作员是否为超级管理员
        validateSuperAdmin(operatorId);

        // 验证目标用户是否存在
        User targetUser = userMapper.selectById(request.getUserId());
        if (targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证角色是否合法
        String role = request.getRole();
        if (!UserRole.ADMIN.getCode().equals(role) && !UserRole.SUPER_ADMIN.getCode().equals(role)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的角色类型");
        }

        // 更新用户角色
        targetUser.setRole(role);
        userMapper.updateById(targetUser);

        log.info("超级管理员 {} 为用户 {} 分配角色: {}", operatorId, request.getUserId(), role);
    }

    /**
     * 取消管理员角色（仅超级管理员可操作）
     *
     * @param operatorId 操作用户ID（超级管理员）
     * @param userId     目标用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeAdminRole(Long operatorId, Long userId) {
        // 验证操作员是否为超级管理员
        validateSuperAdmin(operatorId);

        // 验证目标用户是否存在
        User targetUser = userMapper.selectById(userId);
        if (targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 不能取消自己的管理员角色
        if (operatorId.equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能取消自己的管理员角色");
        }

        // 更新为普通用户
        targetUser.setRole(UserRole.USER.getCode());
        userMapper.updateById(targetUser);

        log.info("超级管理员 {} 取消用户 {} 的管理员角色", operatorId, userId);
    }

    /**
     * 设置API免费策略（仅超级管理员可操作）
     *
     * @param operatorId 操作用户ID（超级管理员）
     * @param request    设置请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void setFreeApiStrategy(Long operatorId, SetFreeApiStrategyRequest request) {
        // 验证操作员是否为超级管理员
        validateSuperAdmin(operatorId);

        // 验证目标用户是否存在
        User targetUser = userMapper.selectById(request.getUserId());
        if (targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证策略是否合法
        String strategy = request.getFreeApiStrategy();
        if (FreeApiStrategy.fromCode(strategy) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的免费策略");
        }

        // 更新免费策略
        targetUser.setFreeApiStrategy(strategy);
        targetUser.setFreeQuota(request.getFreeQuota());
        targetUser.setDailyCallLimit(request.getDailyCallLimit());
        targetUser.setMonthlyCallLimit(request.getMonthlyCallLimit());
        targetUser.setFreeStrategyStartTime(request.getFreeStrategyStartTime());
        targetUser.setFreeStrategyEndTime(request.getFreeStrategyEndTime());
        targetUser.setAllowedFreeModels(request.getAllowedFreeModels());

        userMapper.updateById(targetUser);

        log.info("超级管理员 {} 为用户 {} 设置免费策略: {}", operatorId, request.getUserId(), strategy);
    }

    /**
     * 获取所有管理员列表（仅超级管理员可操作）
     *
     * @param operatorId 操作用户ID（超级管理员）
     * @return 管理员列表
     */
    public List<AdminInfoVO> getAllAdmins(Long operatorId) {
        // 验证操作员是否为超级管理员
        validateSuperAdmin(operatorId);

        // 查询所有管理员和超级管理员
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getRole, UserRole.ADMIN.getCode(), UserRole.SUPER_ADMIN.getCode());
        List<User> admins = userMapper.selectList(wrapper);

        return admins.stream()
                .map(this::convertToAdminVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有用户列表（管理员及以上可操作）
     *
     * @param operatorId 操作用户ID
     * @return 用户列表
     */
    public List<AdminInfoVO> getAllUsers(Long operatorId) {
        // 验证操作员是否为管理员或超级管理员
        validateAdminOrSuperAdmin(operatorId);

        // 查询所有用户
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());

        return users.stream()
                .map(this::convertToAdminVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户详细信息（管理员及以上可操作）
     *
     * @param operatorId 操作用户ID
     * @param userId     目标用户ID
     * @return 用户详细信息
     */
    public AdminInfoVO getUserDetail(Long operatorId, Long userId) {
        // 验证操作员是否为管理员或超级管理员
        validateAdminOrSuperAdmin(operatorId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        return convertToAdminVO(user);
    }

    /**
     * 禁用/启用用户（管理员及以上可操作）
     *
     * @param operatorId 操作用户ID
     * @param userId     目标用户ID
     * @param status     状态：0-禁用，1-启用
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long operatorId, Long userId, Integer status) {
        // 验证操作员是否为管理员或超级管理员
        validateAdminOrSuperAdmin(operatorId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setStatus(status);
        userMapper.updateById(user);

        log.info("管理员 {} {} 用户 {}: status={}", operatorId,
                status == 1 ? "启用" : "禁用", userId, status);
    }

    /**
     * 转换为AdminInfoVO
     *
     * @param user 用户实体
     * @return AdminInfoVO
     */
    private AdminInfoVO convertToAdminVO(User user) {
        AdminInfoVO vo = new AdminInfoVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    /**
     * 获取系统统计信息（管理员及以上可操作）
     *
     * @param operatorId 操作用户ID
     * @return 系统统计信息
     */
    public SystemStatsVO getSystemStats(Long operatorId) {
        // 验证操作员是否为管理员或超级管理员
        validateAdminOrSuperAdmin(operatorId);

        SystemStatsVO stats = new SystemStatsVO();

        // 用户统计
        stats.setTotalUsers(userMapper.selectCount(null));

        LambdaQueryWrapper<User> disabledWrapper = new LambdaQueryWrapper<>();
        disabledWrapper.eq(User::getStatus, 0);
        stats.setDisabledUsers(userMapper.selectCount(disabledWrapper));

        LambdaQueryWrapper<User> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(User::getRole, UserRole.ADMIN.getCode());
        stats.setAdminCount(userMapper.selectCount(adminWrapper));

        LambdaQueryWrapper<User> superAdminWrapper = new LambdaQueryWrapper<>();
        superAdminWrapper.eq(User::getRole, UserRole.SUPER_ADMIN.getCode());
        stats.setSuperAdminCount(userMapper.selectCount(superAdminWrapper));

        // 使用免费策略的用户数
        LambdaQueryWrapper<User> freeStrategyWrapper = new LambdaQueryWrapper<>();
        freeStrategyWrapper.isNotNull(User::getFreeApiStrategy);
        stats.setFreeStrategyUsers(userMapper.selectCount(freeStrategyWrapper));

        // TODO: 活跃用户数需要结合call_log表查询，这里暂时设为0
        stats.setActiveUsers(0L);

        // API调用统计
        stats.setTotalApiCalls(callLogMapper.selectCount(null));

        // 今日API调用次数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<com.ai.gateway.entity.CallLog> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(com.ai.gateway.entity.CallLog::getCreateTime, todayStart);
        stats.setTodayApiCalls(callLogMapper.selectCount(todayWrapper));

        // 消费金额统计 - 简化处理，实际应该注入BillingRecordMapper
        stats.setTotalRevenue(BigDecimal.ZERO);
        stats.setTodayRevenue(BigDecimal.ZERO);
        stats.setAvgCallCost(BigDecimal.ZERO);

        // 模型统计
        stats.setModelCount(modelConfigMapper.selectCount(null));

        LambdaQueryWrapper<com.ai.gateway.entity.ModelConfig> activeModelWrapper = new LambdaQueryWrapper<>();
        activeModelWrapper.eq(com.ai.gateway.entity.ModelConfig::getStatus, 1);
        stats.setActiveModelCount(modelConfigMapper.selectCount(activeModelWrapper));

        return stats;
    }
}
