package com.ai.gateway.config;

import com.ai.gateway.interceptor.ApiKeyAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 
 * @author AI Gateway Platform
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;

    /**
     * 添加拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // API Key鉴权拦截器，拦截所有/chat/**路径
        registry.addInterceptor(apiKeyAuthInterceptor)
                .addPathPatterns("/chat/**")
                .excludePathPatterns("/auth/**", "/user/**");
    }
}
