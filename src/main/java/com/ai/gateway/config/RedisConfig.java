package com.ai.gateway.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * <p>
 * 本类用于配置Spring Data Redis的RedisTemplate，主要功能包括：
 * 1. 配置Key和Value的序列化方式
 * 2. 使用Jackson2进行JSON序列化，支持复杂对象存储
 * 3. 使用String序列化器处理Key，保证Key的可读性
 * </p>
 *
 * <p><b>序列化策略说明：</b></p>
 * <ul>
 *   <li><b>Key序列化：</b>使用StringRedisSerializer，保证Key以纯字符串形式存储，便于查看和管理</li>
 *   <li><b>Value序列化：</b>使用Jackson2JsonRedisSerializer，将Java对象序列化为JSON格式存储</li>
 *   <li><b>Hash Key序列化：</b>使用StringRedisSerializer，与Key保持一致</li>
 *   <li><b>Hash Value序列化：</b>使用Jackson2JsonRedisSerializer，与Value保持一致</li>
 * </ul>
 *
 * <p><b>为什么选择这种序列化方式？</b></p>
 * <ul>
 *   <li>JSON格式人类可读，便于调试和问题排查</li>
 *   <li>跨语言兼容，其他系统也可以读取Redis数据</li>
 *   <li>Jackson性能优秀，支持复杂的对象结构</li>
 *   <li>String格式的Key便于使用Redis命令行工具查询</li>
 * </ul>
 *
 * @author AI Gateway Platform
 * @version 1.0.0
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate，使用JSON序列化
     * <p>
     * 本方法创建一个自定义的RedisTemplate Bean，覆盖Spring Boot的默认配置。
     * 主要目的是改变默认的JDK序列化方式为JSON序列化方式。
     * </p>
     *
     * <p><b>配置步骤：</b></p>
     * <ol>
     *   <li>创建RedisTemplate实例并设置连接工厂</li>
     *   <li>配置ObjectMapper，启用类型信息以支持多态序列化</li>
     *   <li>创建Jackson2JsonRedisSerializer用于Value序列化</li>
     *   <li>创建StringRedisSerializer用于Key序列化</li>
     *   <li>分别设置Key、Value、HashKey、HashValue的序列化器</li>
     *   <li>调用afterPropertiesSet()完成初始化</li>
     * </ol>
     *
     * @param connectionFactory Redis连接工厂，由Spring Boot自动配置注入
     * @return 配置好的RedisTemplate实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建RedisTemplate实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置Redis连接工厂，用于获取Redis连接
        template.setConnectionFactory(connectionFactory);

        // ============================================
        // 配置Value序列化器（Jackson2 JSON序列化）
        // ============================================
        // 创建Jackson2JsonRedisSerializer，用于将Java对象序列化为JSON
        // Object.class表示可以序列化任意类型的对象
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);

        // 创建并配置ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        // 设置可见性：允许序列化所有访问级别的字段（public、private等）
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 激活默认类型信息，使得反序列化时能够正确还原对象类型
        // 这对于存储多态对象（如接口、抽象类的实现）非常重要
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        // 将配置好的ObjectMapper设置到序列化器中
        serializer.setObjectMapper(mapper);

        // ============================================
        // 配置Key序列化器（String序列化）
        // ============================================
        // 创建StringRedisSerializer，用于将Key序列化为字符串
        // 优点：Key在Redis中以纯文本形式存储，便于查看和调试
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // ============================================
        // 应用序列化配置
        // ============================================
        // 设置普通Key的序列化方式（如：SET key value）
        template.setKeySerializer(stringSerializer);
        // 设置Hash结构中field的序列化方式（如：HSET key field value）
        template.setHashKeySerializer(stringSerializer);
        // 设置普通Value的序列化方式
        template.setValueSerializer(serializer);
        // 设置Hash结构中value的序列化方式
        template.setHashValueSerializer(serializer);

        // 完成属性设置后的初始化工作
        // 这个方法会验证配置的正确性并进行必要的初始化
        template.afterPropertiesSet();

        // 返回配置好的RedisTemplate实例
        return template;
    }
}
