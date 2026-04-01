/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.condition;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.expression.CompiledExpression;
import com.sipc115.helix.integration.workflow.runtime.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 条件节点执行器
 * <p>
 * 负责执行工作流的条件节点，根据条件表达式的结果确定执行路径。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class ConditionNodeExecutor implements WorkflowNodeExecutor {
    
    private final TransitionResolver transitionResolver;

    @Autowired
    public ConditionNodeExecutor(TransitionResolver transitionResolver) {
        this.transitionResolver = transitionResolver;
    }

    @Override
    public boolean supports(String type) {
        return DslNodeType.CONDITION.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        String branchKey;
        
        // ⭐ 优先使用编译后的表达式进行判断
        Map<String, Object> config = node.getConfig();
        Object compiledExprObj = config.get("compiledExpression");
        
        if (compiledExprObj instanceof CompiledExpression) {
            try {
                // 执行真正的条件表达式计算
                CompiledExpression compiledExpr = (CompiledExpression) compiledExprObj;
                Boolean result = (Boolean) compiledExpr.execute(context.getVariables());
                branchKey = result ? "true" : "false";
            } catch (Exception e) {
                // 表达式执行失败，降级到默认分支
                branchKey = String.valueOf(config.getOrDefault("defaultBranch", "true"));
            }
        } else {
            // 没有编译后的表达式，使用默认分支
            branchKey = String.valueOf(config.getOrDefault("defaultBranch", "true"));
        }
        
        NodeExecutionResult result = NodeExecutionResult.completed();
        result.setBranchKey(branchKey);
        
        return result;
    }
}
