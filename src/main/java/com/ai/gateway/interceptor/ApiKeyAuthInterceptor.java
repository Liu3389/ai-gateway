package com.ai.gateway.interceptor;

import cn.hutool.json.JSONUtil;
import com.ai.gateway.common.Constants;
import com.ai.gateway.common.Result;
import com.ai.gateway.common.ResultCode;
import com.ai.gateway.entity.ApiKey;
import com.ai.gateway.entity.User;
import com.ai.gateway.exception.BusinessException;
import com.ai.gateway.service.ApiKeyService;
import com.ai.gateway.service.BillingService;
import com.ai.gateway.service.RateLimitService;
import com.ai.gateway.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * API Key鉴权拦截器
 * <p>
 * 本拦截器是AI网关平台的核心安全组件，负责所有API请求的身份认证和前置检查。
 * 在请求到达Controller之前，执行以下验证步骤：
 * 1. API Key验证：检查API Key的有效性和状态
 * 2. 用户状态检查：验证用户账户是否正常
 * 3. 限流检查：防止单个API Key过度使用系统资源
 * 4. 余额检查：确保用户有足够的余额支付API调用费用
 * 5. 预扣余额：预先扣除估算费用，防止超额消费
 * </p>
 *
 * <p><b>拦截器工作流程：</b></p>
 * <ol>
 *   <li>从HTTP Header中获取X-API-Key</li>
 *   <li>验证API Key是否存在且有效</li>
 *   <li>查询关联的用户信息</li>
 *   <li>检查用户账户状态（是否被禁用）</li>
 *   <li>执行限流检查（基于Redis的计数器算法）</li>
 *   <li>检查用户免费策略（UNLIMITED用户跳过余额检查）</li>
 *   <li>验证用户余额是否充足</li>
 *   <li>预扣余额（使用Lua脚本保证原子性）</li>
 *   <li>将用户信息存入request属性，供Controller使用</li>
 *   <li>放行请求，进入Controller处理</li>
 * </ol>
 *
 * <p><b>预扣费机制说明：</b></p>
 * <ul>
 *   <li><b>为什么需要预扣？</b>防止用户在API调用过程中余额不足，导致系统损失</li>
 *   <li><b>预扣多少？</b>目前固定预扣0.1美元（估算值），实际费用在请求结束后结算</li>
 *   <li><b>如何结算？</b>请求完成后，根据实际Token使用量计算真实费用，退还差额</li>
 *   <li><b>失败如何处理？</b>如果API调用失败，预扣金额会全额退还（结算金额为0）</li>
 *   <li><b>幂等性保证：</b>使用requestId作为唯一标识，防止重复预扣</li>
 * </ul>
 *
 * <p><b>免费策略支持：</b></p>
 * <ul>
 *   <li><b>UNLIMITED：</b>完全免费，跳过余额检查和预扣</li>
 *   <li><b>QUOTA_BASED：</b>基于额度的免费，需要检查余额</li>
 *   <li><b>COUNT_LIMITED：</b>基于次数的免费，需要检查调用次数</li>
 *   <li><b>MODEL_SPECIFIC：</b>特定模型免费，需要检查模型类型</li>
 * </ul>
 *
 * <p><b>错误处理：</b></p>
 * <ul>
 *   <li>API Key为空或无效：返回401错误</li>
 *   <li>用户被禁用：返回403错误</li>
 *   <li>超过限流阈值：返回429错误</li>
 *   <li>余额不足：返回402错误</li>
 *   <li>系统异常：返回500错误</li>
 * </ul>
 *
 * <p><b>性能优化：</b></p>
 * <ul>
 *   <li>使用Redis缓存API Key和用户信息，减少数据库查询</li>
 *   <li>使用Lua脚本执行预扣操作，保证原子性并减少网络往返</li>
 *   <li>详细的日志记录，便于问题追踪和性能分析</li>
 * </ul>
 *
 * @author AI Gateway Platform
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    private final ApiKeyService apiKeyService;      // API Key服务，用于验证API Key
    private final UserService userService;          // 用户服务，用于查询用户信息
    private final RateLimitService rateLimitService; // 限流服务，用于检查请求频率
    private final BillingService billingService;    // 计费服务，用于余额检查和预扣

    /**
     * 预处理方法 - 在Controller执行前调用
     * <p>
     * 这是拦截器的核心方法，负责执行所有的身份认证和前置检查。
     * 只有当所有检查都通过时，才会返回true放行请求。
     * </p>
     *
     * <p><b>执行流程详解：</b></p>
     * <ol>
     *   <li><b>获取API Key：</b>从HTTP Header中读取X-API-Key</li>
     *   <li><b>验证API Key：</b>检查API Key是否存在、有效、未过期、已启用</li>
     *   <li><b>查询用户：</b>根据API Key关联的userId查询用户信息</li>
     *   <li><b>检查用户状态：</b>验证用户账户是否被禁用</li>
     *   <li><b>限流检查：</b>检查当前API Key的请求频率是否超过限制</li>
     *   <li><b>免费策略检查：</b>判断用户是否有免费策略（如UNLIMITED）</li>
     *   <li><b>余额检查：</b>验证用户余额是否充足（免费用户跳过）</li>
     *   <li><b>预扣余额：</b>预先扣除估算费用，防止超额消费（免费用户跳过）</li>
     *   <li><b>设置属性：</b>将userId、apiKey、requestId等存入request属性</li>
     *   <li><b>放行请求：</b>返回true，请求进入Controller处理</li>
     * </ol>
     *
     * <p><b>返回值说明：</b></p>
     * <ul>
     *   <li>true - 所有检查通过，放行请求，进入Controller</li>
     *   <li>false - 某项检查失败，已写入错误响应，终止请求处理</li>
     * </ul>
     *
     * <p><b>异常处理：</b></p>
     * <ul>
     *   <li>BusinessException - 业务异常，如API Key无效、余额不足等，返回对应错误码</li>
     *   <li>Exception - 系统异常，如数据库连接失败等，返回500错误</li>
     * </ul>
     *
     * @param request  HTTP请求对象，用于获取Header和设置属性
     * @param response HTTP响应对象，用于写入错误响应
     * @param handler  被调用的处理器（Controller方法）
     * @return true表示放行请求，false表示终止请求
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        log.info("========== API Key鉴权拦截器开始 ==========");
        
        // 获取API Key
        String apiKey = request.getHeader(Constants.HEADER_API_KEY);
        log.info("请求路径: {}, API Key: {}", request.getRequestURI(), apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) + "..." : "null");
        
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("API Key为空");
            writeErrorResponse(response, ResultCode.INVALID_API_KEY);
            return false;
        }

        try {
            // 1. 验证API Key
            log.info("步骤1: 验证API Key...");
            ApiKey apiKeyEntity = apiKeyService.validateApiKey(apiKey);
            log.info("API Key验证通过: id={}, userId={}", apiKeyEntity.getId(), apiKeyEntity.getUserId());
            
            // 2. 查询用户信息
            log.info("步骤2: 查询用户信息...");
            User user = userService.getUserEntityById(apiKeyEntity.getUserId());
            log.info("用户信息查询成功: id={}, username={}, status={}", user.getId(), user.getUsername(), user.getStatus());

            // 检查用户状态
            if (user.getStatus() == Constants.USER_STATUS_DISABLED) {
                log.warn("用户已被禁用: userId={}", user.getId());
                writeErrorResponse(response, ResultCode.USER_DISABLED);
                return false;
            }

            // 3. 限流检查
            log.info("步骤3: 限流检查...");
            if (!rateLimitService.allowRequest(apiKey, apiKeyEntity.getRateLimit())) {
                log.warn("限流检查失败: apiKey={}, rateLimit={}", apiKey, apiKeyEntity.getRateLimit());
                writeErrorResponse(response, ResultCode.RATE_LIMIT_EXCEEDED);
                return false;
            }
            log.info("限流检查通过");

            // 4. 余额检查和预扣（估算最大费用）
            log.info("步骤4: 余额检查...");

            // 检查用户免费策略
            String freeStrategy = user.getFreeApiStrategy();
            boolean isUnlimited = "UNLIMITED".equals(freeStrategy);

            if (!isUnlimited) {
                BigDecimal userBalance = billingService.getUserBalance(user.getId());
                log.info("当前余额: userId={}, balance={}", user.getId(), userBalance);

                if (userBalance.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("余额不足: userId={}, balance={}", user.getId(), userBalance);
                    writeErrorResponse(response, ResultCode.INSUFFICIENT_BALANCE);
                    return false;
                }
            } else {
                log.info("用户拥有完全免费策略，跳过余额检查: userId={}", user.getId());
            }

            // 预扣余额（估算值，实际费用在请求结束后结算）
            String requestId = UUID.randomUUID().toString().replace("-", "");
            BigDecimal preDeductAmount = isUnlimited ? BigDecimal.ZERO : new BigDecimal("0.1"); // 免费用户预扣0

            log.info("步骤5: 开始预扣余额: userId={}, requestId={}, amount={}, freeStrategy={}",
                    user.getId(), requestId, preDeductAmount, freeStrategy);

            if (!isUnlimited) {
                boolean preDeductSuccess = billingService.preDeductBalance(
                        user.getId(), requestId, preDeductAmount, 300);

                log.info("预扣结果: success={}", preDeductSuccess);

                if (!preDeductSuccess) {
                    log.warn("预扣余额失败: userId={}, requestId={}", user.getId(), requestId);
                    writeErrorResponse(response, ResultCode.INSUFFICIENT_BALANCE);
                    return false;
                }

                log.info("预扣余额成功: userId={}, requestId={}", user.getId(), requestId);
            } else {
                log.info("免费用户跳过预扣: userId={}, requestId={}", user.getId(), requestId);
            }

            // 将用户信息和请求ID存入request属性，供后续使用
            request.setAttribute("userId", user.getId());
            request.setAttribute("apiKey", apiKey);
            request.setAttribute("requestId", requestId);
            request.setAttribute("preDeductAmount", preDeductAmount);

            log.info("========== API Key鉴权通过 ==========");
            return true;

        } catch (BusinessException e) {
            log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage());
            writeErrorResponse(response, e.getCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("API Key鉴权异常", e);
            writeErrorResponse(response, ResultCode.ERROR);
            return false;
        }
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode) 
            throws IOException {
        writeErrorResponse(response, resultCode.getCode(), resultCode.getMessage());
    }

    private void writeErrorResponse(HttpServletResponse response, Integer code, String message) 
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        Result<Void> result = Result.error(code, message);
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }
}
