package com.ai.gateway.interceptor;

import cn.hutool.json.JSONUtil;
import com.ai.gateway.common.Constants;
import com.ai.gateway.common.Result;
import com.ai.gateway.common.ResultCode;
import com.ai.gateway.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String token = request.getHeader(Constants.HEADER_USER_TOKEN);
        if (token == null || token.isEmpty()) {
            log.warn("请求缺少Token: uri={}", request.getRequestURI());
            writeErrorResponse(response, ResultCode.UNAUTHORIZED.getCode(), "请先登录");
            return false;
        }

        try {
            Long userId = userService.validateToken(token);
            request.setAttribute("currentUserId", userId);
            return true;
        } catch (Exception e) {
            log.warn("Token验证失败: uri={}", request.getRequestURI(), e);
            writeErrorResponse(response, ResultCode.UNAUTHORIZED.getCode(), "Token无效或已过期，请重新登录");
            return false;
        }
    }

    private void writeErrorResponse(HttpServletResponse response, Integer code, String message)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        Result<Void> result = Result.error(code, message);
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }
}
