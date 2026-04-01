/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.condition;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.expression.CompiledExpression;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 条件节点编译器
 * <p>
 * 负责编译 CONDITION 类型的节点，处理条件表达式的编译和验证。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class ConditionNodeCompiler implements NodeCompiler {
    
    private static final Logger logger = LoggerFactory.getLogger(ConditionNodeCompiler.class);
    
    @Override
    public DslNodeType supportType() {
        return DslNodeType.CONDITION;
    }
    
    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        Map<String, Object> config = source.getConfig();
        if (config == null) {
            throw new IllegalArgumentException("Condition node config cannot be null: " + source.getId());
        }
        
        Object condition = config.get("condition");
        if (condition == null || !(condition instanceof String)) {
            throw new IllegalArgumentException("Condition expression must be a non-empty string: " + source.getId());
        }
        
        String conditionStr = (String) condition;
        if (conditionStr.isEmpty()) {
            throw new IllegalArgumentException("Condition expression cannot be empty: " + source.getId());
        }
    }
    
    @Override
    public CompiledNode compile(DslNodeSpec source, CompileContext context) {
        CompiledNode node = new CompiledNode();
        node.setId(source.getId());
        node.setType(source.getType());
        
        Map<String, Object> config = source.getConfig();
        String conditionStr = (String) config.get("condition");
        
        try {
            // 验证表达式语法
            context.getExpressionEngine().validateExpression(conditionStr);
            
            // 归一化表达式
            String normalizedCondition = normalizeExpression(conditionStr);
            config.put("condition", normalizedCondition);
            
            // 编译表达式并存储编译后的对象
            CompiledExpression compiledExpr = context.getExpressionEngine().compile(normalizedCondition);
            config.put("compiledExpression", compiledExpr);
            config.put("compiled", true);
            config.put("expressionLanguage", context.getExpressionEngine().getEngineName());
            
            logger.debug("Compiled condition expression for node {}: {}", source.getId(), normalizedCondition);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid condition expression for CONDITION node " + source.getId() + ": " + e.getMessage());
        }
        
        node.setConfig(config);
        node.setAction("CONDITION");
        return node;
    }
    
    /**
     * 归一化表达式
     * <p>
     * 移除表达式中的多余空格，保留必要的空格。
     * 
     * @param expression 表达式字符串
     * @return 归一化后的表达式
     */
    private String normalizeExpression(String expression) {
        // 移除多余的空格，保留必要的空格
        return expression.trim().replaceAll("\\s+  ", " ");
    }
}
