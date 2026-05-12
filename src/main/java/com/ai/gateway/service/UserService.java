package com.ai.gateway.service;

import com.ai.gateway.common.Constants;
import com.ai.gateway.common.ResultCode;
import com.ai.gateway.common.UserRole;
import com.ai.gateway.dto.UserLoginRequest;
import com.ai.gateway.dto.UserRegisterRequest;
import com.ai.gateway.entity.User;
import com.ai.gateway.exception.BusinessException;
import com.ai.gateway.mapper.UserMapper;
import com.ai.gateway.vo.UserInfoVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务类
 * 
 * @author AI Gateway Platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BillingService billingService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 用户注册
     * 
     * @param request 注册请求
     * @return 用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO register(UserRegisterRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User existingUser = userMapper.selectOne(wrapper);
        
        if (existingUser != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        // 使用BCrypt加密密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setBalance(BigDecimal.ZERO);
        user.setStatus(1);
        user.setRole(UserRole.USER.getCode()); // 默认为普通用户

        userMapper.insert(user);
        
        // 同步余额到Redis
        billingService.syncUserBalanceToRedis(user.getId(), user.getBalance());

        log.info("用户注册成功: username={}, userId={}", request.getUsername(), user.getId());
        
        return convertToVO(user);
    }

    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 用户信息
     */
    public UserInfoVO login(UserLoginRequest request) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 缓存用户角色到Redis（24小时过期）
        String roleKey = Constants.REDIS_USER_ROLE_PREFIX + user.getId();
        stringRedisTemplate.opsForValue().set(roleKey, user.getRole(), 24, TimeUnit.HOURS);

        log.info("用户登录成功: username={}, userId={}", request.getUsername(), user.getId());
        
        return convertToVO(user);
    }

    /**
     * 查询用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        return convertToVO(user);
    }

    /**
     * 根据ID获取用户实体
     * 
     * @param userId 用户ID
     * @return 用户实体
     */
    public User getUserEntityById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 查询用户余额
     * 
     * @param userId 用户ID
     * @return 余额
     */
    public BigDecimal getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        return user.getBalance();
    }

    /**
     * 充值
     * 
     * @param userId 用户ID
     * @param amount 充值金额
     */
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long userId, BigDecimal amount) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        BigDecimal balanceBefore = user.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        
        user.setBalance(balanceAfter);
        userMapper.updateById(user);
        
        // 同步到Redis
        billingService.syncUserBalanceToRedis(userId, balanceAfter);

        log.info("用户充值成功: userId={}, amount={}, balanceBefore={}, balanceAfter={}", 
                userId, amount, balanceBefore, balanceAfter);
    }

    /**
     * 更新数据库余额（异步结算时调用）
     * 
     * @param userId 用户ID
     * @param newBalance 新余额
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateBalance(Long userId, BigDecimal newBalance) {
        User user = new User();
        user.setId(userId);
        user.setBalance(newBalance);
        userMapper.updateById(user);
        
        log.debug("更新用户余额: userId={}, newBalance={}", userId, newBalance);
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 转换为VO对象
     * 
     * @param user 用户实体
     * @return 用户信息VO
     */
    private UserInfoVO convertToVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
