package com.sipc115.helix.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置类
 * <p>
 * 配置 Jackson ObjectMapper，添加 Java 时间模块支持
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    /**
     * 创建 ObjectMapper 实例
     * 
     * @return ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册 Java 时间模块，支持 LocalDate、LocalDateTime 等时间类型
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
