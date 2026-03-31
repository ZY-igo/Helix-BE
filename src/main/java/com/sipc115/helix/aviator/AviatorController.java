/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.aviator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Aviator 表达式引擎示例控制器
 * <p>
 * 展示如何在项目中使用 Aviator 表达式引擎，提供表达式执行的 REST 接口。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/aviator")
public class AviatorController {

    @Autowired
    private AviatorService aviatorService;

    /**
     * 执行表达式
     * <p>
     * 接收表达式字符串和执行环境，返回执行结果。
     * 
     * @param request 包含表达式和环境的请求
     * @return 表达式执行结果
     */
    @PostMapping("/execute")
    public Object execute(@RequestBody AviatorRequest request) {
        return aviatorService.execute(request.getExpression(), request.getEnv());
    }

    /**
     * 执行布尔表达式
     * <p>
     * 接收布尔表达式字符串和执行环境，返回布尔执行结果。
     * 
     * @param request 包含表达式和环境的请求
     * @return 布尔表达式执行结果
     */
    @PostMapping("/execute-boolean")
    public boolean executeBoolean(@RequestBody AviatorRequest request) {
        return aviatorService.executeBoolean(request.getExpression(), request.getEnv());
    }

    /**
     * 执行数值表达式
     * <p>
     * 接收数值表达式字符串和执行环境，返回数值执行结果。
     * 
     * @param request 包含表达式和环境的请求
     * @return 数值表达式执行结果
     */
    @PostMapping("/execute-number")
    public Number executeNumber(@RequestBody AviatorRequest request) {
        return aviatorService.executeNumber(request.getExpression(), request.getEnv());
    }

    /**
     * Aviator 请求对象
     * <p>
     * 用于接收表达式执行请求的参数。
     */
    public static class AviatorRequest {
        private String expression;
        private Map<String, Object> env;

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public Map<String, Object> getEnv() {
            return env;
        }

        public void setEnv(Map<String, Object> env) {
            this.env = env;
        }
    }
}
