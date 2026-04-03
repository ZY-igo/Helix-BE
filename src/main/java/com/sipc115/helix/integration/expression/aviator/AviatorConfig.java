/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.expression.aviator;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Aviator 表达式引擎配置类
 * <p>
 * 用于初始化和配置 Aviator 表达式引擎，设置全局选项和自定义函数。
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Configuration
public class AviatorConfig {

    /**
     * 初始化 Aviator 引擎
     * <p>
     * 在应用启动时执行，配置 Aviator 的全局选项。
     */
    @PostConstruct
    public void init() {
        // 注册自定义函数
        registerCustomFunctions();
    }

    /**
     * 注册自定义函数
     * <p>
     * 可以在这里注册项目特定的自定义函数，扩展 Aviator 的能力。
     */
    private void registerCustomFunctions() {
        // 示例：注册一个自定义函数
        // AviatorEvaluator.addFunction(new CustomFunction());
    }
}
