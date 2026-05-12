package com.ai.gateway.common;

/**
 * 系统常量定义类
 * <p>
 * 本类集中定义了AI网关平台使用的所有系统常量，包括：
 * 1. Redis Key前缀 - 用于统一管理和避免Key冲突
 * 2. HTTP Header名称 - 用于API请求的身份认证
 * 3. 时间配置 - Token过期时间、限流窗口等
 * 4. 业务常量 - 限流阈值、计费类型、状态码等
 * </p>
 * <p>
 * 设计原则：
 * - 所有常量使用public static final修饰，确保不可变性
 * - 按功能分组，便于维护和查找
 * - 使用有意义的命名，提高代码可读性
 * - 提供详细的JavaDoc注释，说明每个常量的用途
 * </p>
 *
 * @author AI Gateway Platform
 * @version 1.0.0
 */
public class Constants {

    // ============================================
    // Redis Key前缀常量
    // 用于统一Redis键名命名规范，避免键名冲突
    // 格式：ai_gateway:{模块}:{具体标识}
    // ============================================

    /**
     * Redis Key前缀 - API Key信息缓存
     * <p>
     * 完整Key格式：ai_gateway:api_key:{apiKey}
     * 存储内容：API Key的详细信息（JSON格式）
     * 用途：快速验证API Key的有效性，避免频繁查询数据库
     * 过期策略：根据API Key的expireTime设置，或永久有效
     * </p>
     */
    public static final String REDIS_API_KEY_PREFIX = "ai_gateway:api_key:";

    /**
     * Redis Key前缀 - 限流计数器
     * <p>
     * 完整Key格式：ai_gateway:rate_limit:{apiKey}
     * 存储内容：当前时间窗口内的请求计数（整数）
     * 用途：实现基于固定窗口的限流算法
     * 过期策略：自动过期，过期时间为限流窗口大小（默认60秒）
     * </p>
     */
    public static final String REDIS_RATE_LIMIT_PREFIX = "ai_gateway:rate_limit:";

    /**
     * Redis Key前缀 - 用户余额缓存
     * <p>
     * 完整Key格式：ai_gateway:user_balance:{userId}
     * 存储内容：用户的当前可用余额（字符串格式，保留6位小数）
     * 用途：高性能余额查询和扣减操作，避免数据库频繁IO
     * 过期策略：永久有效，通过同步机制保证与数据库一致性
     * </p>
     */
    public static final String REDIS_USER_BALANCE_PREFIX = "ai_gateway:user_balance:";

    /**
     * Redis Key前缀 - 计费预扣记录
     * <p>
     * 完整Key格式：ai_gateway:billing_pre_deduct:{requestId}
     * 存储内容：预扣金额（字符串格式）
     * 用途：
     *   1. 幂等性检查：防止同一请求重复预扣
     *   2. 结算依据：后续根据预扣金额和实际消费进行结算
     *   3. 回滚参考：如果请求失败，可以根据预扣记录回滚余额
     * 过期策略：临时存储，结算后删除，或超时自动删除
     * </p>
     */
    public static final String REDIS_BILLING_PRE_DEDUCT_PREFIX = "ai_gateway:billing_pre_deduct:";

    /**
     * Redis Key前缀 - 用户角色缓存
     * <p>
     * 完整Key格式：ai_gateway:user_role:{userId}
     * 存储内容：用户角色字符串（USER/ADMIN/SUPER_ADMIN）
     * 用途：快速验证用户权限，避免频繁查询数据库
     * 过期策略：24小时过期，与Token生命周期一致
     * </p>
     */
    public static final String REDIS_USER_ROLE_PREFIX = "ai_gateway:user_role:";

    /**
     * Redis Key前缀 - 登录Token
     * <p>
     * 完整Key格式：ai_gateway:token:{token}
     * 存储内容：用户ID（字符串格式）
     * 用途：验证用户登录状态，实现无状态认证
     * 过期策略：24小时过期，用户活跃时可续期
     * </p>
     */
    public static final String REDIS_TOKEN_PREFIX = "ai_gateway:token:";

    // ============================================
    // HTTP Header常量
    // 用于API请求的身份认证和信息传递
    // ============================================

    /**
     * HTTP Header - API Key
     * <p>
     * Header名称：X-API-Key
     * 用途：API调用时的身份认证，识别调用者身份
     * 使用场景：所有需要认证的API接口（如/chat/**）
     * 示例：curl -H "X-API-Key: sk-xxx" http://localhost:8080/api/chat/completions
     * </p>
     */
    public static final String HEADER_API_KEY = "X-API-Key";

    /**
     * HTTP Header - 用户Token
     * <p>
     * Header名称：X-User-Token
     * 用途：用户登录后的身份认证，用于访问用户相关接口
     * 使用场景：用户管理接口（如/auth/userinfo, /user/recharge）
     * 示例：curl -H "X-User-Token: xxx" http://localhost:8080/api/auth/userinfo?userId=1
     * </p>
     */
    public static final String HEADER_USER_TOKEN = "X-User-Token";

    /**
     * HTTP Header - 操作者用户ID（Admin用）
     * <p>
     * Header名称：X-User-Id
     * 用途：管理员操作时标识操作者身份，用于审计日志
     * 使用场景：管理员接口（如/admin/**）
     * 示例：curl -H "X-User-Id: 1" http://localhost:8080/api/admin/stats
     * </p>
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    // ============================================
    // 时间配置常量
    // ============================================

    /**
     * Token过期时间（24小时）
     * <p>
     * 用途：用户登录后生成的Token有效期
     * 单位：小时
     * 设计考虑：
     *   - 24小时平衡了安全性和用户体验
     *   - 过短会导致频繁登录，影响体验
     *   - 过长会增加安全风险
     * </p>
     */
    public static final Long TOKEN_EXPIRE_HOURS = 24L;

    // ============================================
    // 限流配置常量
    // ============================================

    /**
     * 默认限流阈值（每分钟请求数）
     * <p>
     * 用途：单个API Key每分钟允许的最大请求数
     * 默认值：100次/分钟
     * 可配置：可通过ApiKey.rateLimit字段自定义
     * 设计考虑：
     *   - 防止单个用户过度占用系统资源
     *   - 保护后端AI模型API不被滥用
     *   - 保证系统的稳定性和公平性
     * </p>
     */
    public static final Integer DEFAULT_RATE_LIMIT = 100;

    /**
     * 限流时间窗口（秒）
     * <p>
     * 用途：限流算法的时间窗口大小
     * 默认值：60秒（1分钟）
     * 算法：固定窗口计数器算法
     * 工作原理：
     *   1. 每个时间窗口内维护一个计数器
     *   2. 请求到来时检查计数器是否超过阈值
     *   3. 超过阈值则拒绝请求
     *   4. 时间窗口结束后计数器重置
     * </p>
     */
    public static final Long RATE_LIMIT_WINDOW_SECONDS = 60L;

    // ============================================
    // 计费配置常量
    // ============================================

    /**
     * Token价格计算基数（每千Token）
     * <p>
     * 用途：计算API调用费用的基准单位
     * 计算公式：费用 = (prompt_tokens + completion_tokens) / 1000 * price_per_1k_tokens
     * 示例：如果price_per_1k_tokens=0.002，使用1500 tokens的费用为 1.5 * 0.002 = 0.003美元
     * </p>
     */
    public static final Integer TOKEN_PRICE_BASE = 1000;

    /**
     * 计费类型 - 扣费
     * <p>
     * 值：1
     * 用途：标识这是一条扣费记录（API调用消费）
     * 存储位置：billing_record表的type字段
     * </p>
     */
    public static final Integer BILLING_TYPE_DEDUCT = 1;

    /**
     * 计费类型 - 充值
     * <p>
     * 值：2
     * 用途：标识这是一条充值记录（用户手动充值）
     * 存储位置：billing_record表的type字段
     * </p>
     */
    public static final Integer BILLING_TYPE_RECHARGE = 2;

    // ============================================
    // 状态常量
    // 用于统一系统中各种实体的状态表示
    // ============================================

    /**
     * 调用状态 - 成功
     * <p>
     * 值：1
     * 用途：标识API调用成功完成
     * 存储位置：call_log表的status字段
     * </p>
     */
    public static final Integer CALL_STATUS_SUCCESS = 1;

    /**
     * 调用状态 - 失败
     * <p>
     * 值：0
     * 用途：标识API调用失败（可能是余额不足、限流、模型错误等）
     * 存储位置：call_log表的status字段
     * </p>
     */
    public static final Integer CALL_STATUS_FAILED = 0;

    /**
     * 用户状态 - 启用
     * <p>
     * 值：1
     * 用途：标识用户账户正常，可以登录和使用服务
     * 存储位置：user表的status字段
     * </p>
     */
    public static final Integer USER_STATUS_ENABLED = 1;

    /**
     * 用户状态 - 禁用
     * <p>
     * 值：0
     * 用途：标识用户账户被禁用，无法登录和使用服务
     * 可能原因：违规操作、欠费、管理员手动禁用等
     * 存储位置：user表的status字段
     * </p>
     */
    public static final Integer USER_STATUS_DISABLED = 0;

    /**
     * API Key状态 - 启用
     * <p>
     * 值：1
     * 用途：标识API Key有效，可以用于API调用
     * 存储位置：api_key表的status字段
     * </p>
     */
    public static final Integer API_KEY_STATUS_ENABLED = 1;

    /**
     * API Key状态 - 禁用
     * <p>
     * 值：0
     * 用途：标识API Key已禁用，不能用于API调用
     * 可能原因：用户主动禁用、密钥泄露、管理员禁用等
     * 存储位置：api_key表的status字段
     * </p>
     */
    public static final Integer API_KEY_STATUS_DISABLED = 0;

    /**
     * 模型状态 - 启用
     * <p>
     * 值：1
     * 用途：标识模型可用，用户可以调用
     * 存储位置：model_config表的status字段
     * </p>
     */
    public static final Integer MODEL_STATUS_ENABLED = 1;

    /**
     * 模型状态 - 禁用
     * <p>
     * 值：0
     * 用途：标识模型不可用，用户无法调用
     * 可能原因：模型下线、维护中、成本过高等
     * 存储位置：model_config表的status字段
     * </p>
     */
    public static final Integer MODEL_STATUS_DISABLED = 0;
}
