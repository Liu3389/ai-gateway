package com.ai.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security安全配置类
 * <p>
 * 本类用于配置Spring Security框架，主要功能包括：
 * 1. 配置密码编码器（BCrypt）
 * 2. 配置HTTP安全策略（无状态会话、CSRF禁用等）
 * 3. 配置CORS跨域资源共享策略
 * 4. 配置URL访问权限规则
 * </p>
 *
 * <p><b>重要说明：</b></p>
 * <p>
 * 本系统采用自定义的身份认证机制，而非Spring Security的内置认证：
 * - API Key认证：通过ApiKeyAuthInterceptor拦截器实现
 * - 用户Token认证：通过TokenAuthInterceptor拦截器实现
 * - 管理员权限校验：在Service层通过X-User-Id Header和Redis中的角色信息进行校验
 * </p>
 *
 * <p>
 * 因此，Spring Security在这里主要用于：
 * - 提供密码加密功能（BCrypt）
 * - 配置CORS跨域策略
 * - 作为安全框架的基础设施
 * </p>
 *
 * @author AI Gateway Platform
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 配置密码编码器（使用BCrypt算法）
     * <p>
     * BCrypt是一种强大的密码哈希算法，具有以下特点：
     * 1. 自动加盐：每次加密同一密码都会生成不同的结果
     * 2. 计算慢：故意设计为计算密集型，防止暴力破解
     * 3. 可调强度：可以通过参数调整计算复杂度
     * 4. 不可逆：无法从哈希值还原原始密码
     * </p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>用户注册时：对明文密码进行加密后存储到数据库</li>
     *   <li>用户登录时：将输入的密码与数据库中存储的哈希值进行比对</li>
     * </ul>
     *
     * <p><b>示例：</b></p>
     * <pre>{@code
     * // 加密密码
     * String encodedPassword = passwordEncoder.encode("123456");
     * // 结果类似：$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
     *
     * // 验证密码
     * boolean matches = passwordEncoder.matches("123456", encodedPassword);
     * }</pre>
     *
     * @return BCrypt密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置安全过滤链
     * <p>
     * 本方法定义了整个应用的安全策略，包括：
     * 1. CSRF保护配置
     * 2. 会话管理策略
     * 3. CORS跨域配置
     * 4. URL访问权限规则
     * </p>
     *
     * <p><b>为什么禁用CSRF？</b></p>
     * <p>
     * CSRF（跨站请求伪造）保护主要针对基于Cookie的会话认证。
     * 本系统使用无状态的API Key和Token认证，不依赖Cookie，
     * 因此不需要CSRF保护。禁用CSRF可以简化API调用流程。
     * </p>
     *
     * <p><b>为什么使用无状态会话？</b></p>
     * <p>
     * STATELESS表示Spring Security不会创建或使用HttpSession。
     * 每个请求都必须携带完整的认证信息（API Key或Token）。
     * 这种设计的优势：
     * - 便于水平扩展，无需会话共享
     * - 减少服务器内存占用
     * - 更适合RESTful API架构
     * </p>
     *
     * <p><b>为什么所有端点都使用permitAll()？</b></p>
     * <p>
     * 因为本系统使用自定义的拦截器进行身份认证和权限校验：
     * - ApiKeyAuthInterceptor：校验API Key、限流、余额
     * - TokenAuthInterceptor：校验用户Token
     * - Service层：校验管理员角色
     * Spring Security在这里只提供基础设施，实际的权限控制由自定义逻辑完成。
     * </p>
     *
     * @param http HttpSecurity对象，用于配置安全策略
     * @return 配置好的SecurityFilterChain
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ============================================
                // 禁用CSRF保护
                // ============================================
                // API服务通常不需要CSRF保护，因为：
                // 1. 不使用Cookie进行身份认证
                // 2. 客户端需要显式提供API Key或Token
                // 3. 禁用CSRF可以简化API调用流程
                .csrf(csrf -> csrf.disable())

                // ============================================
                // 配置会话管理策略（无状态）
                // ============================================
                // STATELESS表示不创建或使用HttpSession
                // 每个请求都必须携带完整的认证信息
                // 适合RESTful API和微服务架构
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ============================================
                // 配置CORS跨域策略
                // ============================================
                // 允许前端应用跨域访问API
                // 详细配置见corsConfigurationSource()方法
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ============================================
                // 配置URL访问权限规则
                // ============================================
                // 注意：这里使用permitAll()是因为实际的身份认证和权限校验
                // 由自定义拦截器（ApiKeyAuthInterceptor、TokenAuthInterceptor）
                // 和Service层的业务逻辑完成
                .authorizeHttpRequests(auth -> auth
                        // ----------------------------------------
                        // 公开接口：无需任何认证
                        // ----------------------------------------
                        // /auth/register - 用户注册
                        // /auth/login - 用户登录
                        // 这些接口是用户进入系统的入口，必须公开
                        .requestMatchers("/auth/**").permitAll()

                        // ----------------------------------------
                        // 管理员接口：Service层校验 X-User-Id + 角色
                        // ----------------------------------------
                        // /admin/stats - 系统统计
                        // /admin/users - 用户管理
                        // /admin/set-free-api-strategy - 设置免费策略
                        // 实际权限校验在AdminController和AdminService中完成
                        .requestMatchers("/admin/**").permitAll()

                        // ----------------------------------------
                        // AI对话接口：ApiKeyAuthInterceptor 校验
                        // ----------------------------------------
                        // /chat/completions - 流式/非流式对话
                        // 拦截器会校验：
                        //   1. API Key的有效性
                        //   2. API Key的状态（是否启用）
                        //   3. 限流检查
                        //   4. 余额检查
                        .requestMatchers("/chat/**").permitAll()

                        // ----------------------------------------
                        // API Key管理接口：Service层校验归属
                        // ----------------------------------------
                        // /api-key/generate - 生成API Key
                        // /api-key/list - 查询API Key列表
                        // /api-key/disable - 禁用API Key
                        // 需要在请求中传入userId，Service层会校验API Key是否属于该用户
                        .requestMatchers("/api-key/**").permitAll()

                        // ----------------------------------------
                        // 用户接口：TokenAuthInterceptor 校验
                        // ----------------------------------------
                        // /user/recharge - 用户充值
                        // /user/info - 查询用户信息
                        // 拦截器会校验X-User-Token的有效性
                        .requestMatchers("/user/**").permitAll()

                        // ----------------------------------------
                        // Actuator监控端点：公开访问
                        // ----------------------------------------
                        // /actuator/health - 健康检查
                        // /actuator/info - 应用信息
                        // /actuator/metrics - 性能指标
                        // 这些端点用于运维监控，通常公开访问
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // ----------------------------------------
                        // 其他所有请求：允许访问
                        // ----------------------------------------
                        // 默认策略：允许所有其他请求
                        // 实际的权限控制由自定义拦截器完成
                        .anyRequest().permitAll()
                );

        // 构建并返回SecurityFilterChain
        return http.build();
    }

    /**
     * 配置CORS（跨域资源共享）策略
     * <p>
     * CORS是一种浏览器安全机制，用于控制Web应用如何与不同域名的资源交互。
     * 本配置允许前端应用（如React、Vue等）跨域访问后端API。
     * </p>
     *
     * <p><b>CORS工作原理：</b></p>
     * <ol>
     *   <li>浏览器发送OPTIONS预检请求，询问服务器是否允许跨域</li>
     *   <li>服务器返回CORS响应头，声明允许的源、方法、头部等</li>
     *   <li>浏览器根据响应头决定是否发送实际请求</li>
     * </ol>
     *
     * <p><b>配置说明：</b></p>
     * <ul>
     *   <li><b>allowedOriginPatterns:</b> 允许的源域名模式，"*"表示允许所有域名</li>
     *   <li><b>allowedMethods:</b> 允许的HTTP方法（GET、POST、PUT、DELETE、OPTIONS）</li>
     *   <li><b>allowedHeaders:</b> 允许的请求头部，"*"表示允许所有头部</li>
     *   <li><b>allowCredentials:</b> 是否允许携带凭证（Cookie、Authorization头等）</li>
     *   <li><b>maxAge:</b> 预检请求的缓存时间（秒），减少预检请求次数</li>
     * </ul>
     *
     * <p><b>为什么使用allowedOriginPatterns而不是allowedOrigins？</b></p>
     * <p>
     * 当allowCredentials=true时，不能使用allowedOrigins("*")，这是浏览器的安全限制。
     * allowedOriginPatterns支持通配符模式，可以与allowCredentials同时使用。
     * </p>
     *
     * @return CORS配置源对象
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 创建CORS配置对象
        CorsConfiguration configuration = new CorsConfiguration();

        // ============================================
        // 配置允许的源（Origin）
        // ============================================
        // 使用allowedOriginPatterns替代allowedOrigins以支持allowCredentials
        // "*"表示允许所有域名访问（生产环境建议指定具体域名）
        // 示例：configuration.setAllowedOriginPatterns(List.of("https://example.com"));
        configuration.setAllowedOriginPatterns(List.of("*"));

        // ============================================
        // 配置允许的HTTP方法
        // ============================================
        // GET: 查询数据
        // POST: 创建数据
        // PUT: 更新数据
        // DELETE: 删除数据
        // OPTIONS: CORS预检请求
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // ============================================
        // 配置允许的请求头部
        // ============================================
        // "*"表示允许所有请求头部
        // 常见的自定义头部：X-API-Key、X-User-Token、X-User-Id等
        configuration.setAllowedHeaders(List.of("*"));

        // ============================================
        // 配置是否允许携带凭证
        // ============================================
        // true表示允许携带Cookie、Authorization头等凭证信息
        // 注意：设置为true时，不能使用allowedOrigins("*")
        configuration.setAllowCredentials(true);

        // ============================================
        // 配置预检请求缓存时间
        // ============================================
        // 单位：秒
        // 浏览器会缓存预检请求的结果，在缓存时间内不再发送OPTIONS请求
        // 3600秒 = 1小时，可以减少预检请求次数，提高性能
        configuration.setMaxAge(3600L);

        // ============================================
        // 创建并返回CORS配置源
        // ============================================
        // UrlBasedCorsConfigurationSource基于URL路径匹配CORS配置
        // registerCorsConfiguration("/**", configuration)表示对所有路径应用此配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
