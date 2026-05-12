package com.ai.gateway.interceptor;

import com.ai.gateway.common.Constants;
import com.ai.gateway.common.Result;
import com.ai.gateway.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员权限拦截器
 *
 * @author AI Gateway Platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 获取用户ID（从请求头）
        String userIdHeader = request.getHeader("X-User-Id");

        if (userIdHeader == null || userIdHeader.isEmpty()) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED, "缺少用户ID");
            return false;
        }

        try {
            Long userId = Long.parseLong(userIdHeader);

            // 检查用户角色（从Redis或数据库）
            String roleKey = Constants.REDIS_USER_ROLE_PREFIX + userId;
            String role = stringRedisTemplate.opsForValue().get(roleKey);

            if (role == null) {
                // 如果Redis中没有，需要从数据库查询并缓存
                // 这里简化处理，实际应该注入UserService
                writeErrorResponse(response, ResultCode.FORBIDDEN, "需要管理员权限");
                return false;
            }

            // 验证是否为管理员或超级管理员
            if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
                writeErrorResponse(response, ResultCode.FORBIDDEN, "需要管理员权限");
                return false;
            }

            // 将用户ID存入request属性
            request.setAttribute("userId", userId);
            request.setAttribute("userRole", role);

            return true;

        } catch (NumberFormatException e) {
            writeErrorResponse(response, ResultCode.PARAM_ERROR, "无效的用户ID");
            return false;
        }
    }

    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode, String message)
            throws java.io.IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        Result<Void> result = Result.error(resultCode.getCode(), message);
        response.getWriter().write(cn.hutool.json.JSONUtil.toJsonStr(result));
    }
}
