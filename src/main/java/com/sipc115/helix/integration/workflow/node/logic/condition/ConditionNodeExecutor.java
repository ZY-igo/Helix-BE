/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.logic.condition;

import com.sipc115.helix.common.constant.BranchKeyConstants;
import com.sipc115.helix.common.constant.SystemConfigConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.expression.CompiledExpression;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConditionNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ConditionNodeExecutor.class);

    @Override
    public boolean supports(String type) {
        return DslNodeType.CONDITION.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        String branchKey;
        Map<String, Object> config = node.getConfig();
        Object compiledExprObj = config.get("compiledExpression");

        if (compiledExprObj instanceof CompiledExpression) {
            try {
                CompiledExpression compiledExpr = (CompiledExpression) compiledExprObj;
                Boolean evalResult = (Boolean) compiledExpr.execute(context.getVariables());
                branchKey = evalResult ? BranchKeyConstants.TRUE : BranchKeyConstants.FALSE;
                log.debug("条件表达式求值结果: nodeId={}, result={}, branchKey={}",
                    node.getId(), evalResult, branchKey);
            } catch (Exception e) {
                log.warn("条件表达式求值失败，使用默认分支: nodeId={}, error={}",
                    node.getId(), e.getMessage());
                branchKey = String.valueOf(config.getOrDefault("defaultBranch", SystemConfigConstants.DEFAULT_CONDITION_BRANCH));
            }
        } else {
            log.warn("条件节点缺少编译表达式，使用默认分支: nodeId={}", node.getId());
            branchKey = String.valueOf(config.getOrDefault("defaultBranch", SystemConfigConstants.DEFAULT_CONDITION_BRANCH));
        }

        NodeExecutionResult result = NodeExecutionResult.completed();
        result.setBranchKey(branchKey);

        log.info("条件节点执行完成: nodeId={}, branchKey={}", node.getId(), branchKey);
        return result;
    }
}
