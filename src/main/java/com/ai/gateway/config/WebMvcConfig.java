package com.ai.gateway.config;

import com.ai.gateway.interceptor.ApiKeyAuthInterceptor;
import com.ai.gateway.interceptor.TokenAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;
    private final TokenAuthInterceptor tokenAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyAuthInterceptor)
                .addPathPatterns("/chat/**");

        registry.addInterceptor(tokenAuthInterceptor)
                .addPathPatterns("/user/**", "/api-key/**", "/admin/**")
                .excludePathPatterns("/auth/**");
    }
}
