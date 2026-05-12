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
 * 安全配置类
 *
 * @author AI Gateway Platform
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 密码编码器（使用BCrypt）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（API服务通常不需要）
                .csrf(csrf -> csrf.disable())

                // 无状态会话（API不使用服务端Session）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 配置授权规则
                // 注意：本系统使用自定义拦截器（ApiKeyAuthInterceptor）和Header（X-User-Id）进行身份认证，
                // 而非 Spring Security 的内置认证机制。因此所有业务端点使用 permitAll()，
                // 实际权限校验在拦截器和Service层完成。
                .authorizeHttpRequests(auth -> auth
                        // 公开接口：注册、登录
                        .requestMatchers("/auth/**").permitAll()

                        // 管理员接口：Service层校验 X-User-Id + 角色
                        .requestMatchers("/admin/**").permitAll()

                        // API接口：ApiKeyAuthInterceptor 校验 API Key + 限流 + 余额
                        .requestMatchers("/chat/**").permitAll()

                        // API Key管理接口：需传入 userId，Service层校验归属
                        .requestMatchers("/api-key/**").permitAll()

                        // 用户接口（充值等）
                        .requestMatchers("/user/**").permitAll()

                        // Actuator监控端点
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // 其他所有请求
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 使用 allowedOriginPatterns 替代 allowedOrigins 以支持 allowCredentials
        configuration.setAllowedOriginPatterns(List.of("*"));

        // 允许的方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许的请求头
        configuration.setAllowedHeaders(List.of("*"));

        // 允许携带凭证
        configuration.setAllowCredentials(true);

        // 预检请求缓存时间（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
