/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.compiler;

import com.sipc115.helix.domain.workflow.*;
import com.sipc115.helix.integration.workflow.port.DslCompiler;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 默认 DSL 编译器实现
 * <p>
 * 负责将工作流 DSL 编译为执行计划，包含节点编译和转换处理。
 * 实现了 DslCompiler 接口，提供 DSL 到执行计划的转换功能。
 * <p>
 * 注：当前实现为简化版本，后续需要添加完整的 DSL 校验、条件表达式编译和复杂图编译等功能。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class DefaultDslCompiler implements DslCompiler {
    /**
     * 编译工作流 DSL
     * <p>
     * 将工作流 DSL 转换为执行计划，包含工作流 ID、版本、入口节点、编译后的节点和转换关系。
     * 
     * @param dsl 工作流 DSL 对象
     * @return 编译后的执行计划
     * @throws IllegalArgumentException 当 DSL 中缺少 START 节点时抛出
     */
    @Override
    public ExecutionPlan compile(WorkflowDsl dsl) {
        // TODO 1: 做完整 DSL 校验（start/end 唯一性、环检测、变量引用合法性、节点参数 schema 校验）
        // TODO 2: 做条件表达式编译/归一化
        // TODO 3: 做并行/join/fork 等更复杂的图编译

        // 创建执行计划对象
        ExecutionPlan plan = new ExecutionPlan();
        
        // 设置工作流 ID 和版本
        plan.setWorkflowId(dsl.getWorkflowId());
        plan.setWorkflowVersion(dsl.getVersion());
        
        // 查找并设置入口节点 ID
        plan.setEntryNodeId(findEntryNodeId(dsl));
        
        // 编译节点并转换为映射
        plan.setNodes(dsl.getNodes().stream().map(this::compileNode)
                .collect(Collectors.toMap(CompiledNode::getId, Function.identity())));
        
        // 转换边为转换对象
        plan.setTransitions(dsl.getEdges().stream().map(edge -> {
            Transition transition = new Transition();
            transition.setFrom(edge.getFrom());
            transition.setTo(edge.getTo());
            transition.setConditionKey(edge.getConditionKey());
            return transition;
        }).toList());
        
        return plan;
    }

    /**
     * 查找入口节点 ID
     * <p>
     * 从 DSL 中查找类型为 START 的节点，作为工作流的入口节点。
     * 
     * @param dsl 工作流 DSL 对象
     * @return 入口节点的 ID
     * @throws IllegalArgumentException 当 DSL 中缺少 START 节点时抛出
     */
    private String findEntryNodeId(WorkflowDsl dsl) {
        return dsl.getNodes().stream()
                .filter(node -> node.getType() == DslNodeType.START)
                .findFirst()
                .map(DslNodeSpec::getId)
                .orElseThrow(() -> new IllegalArgumentException("DSL must contain START node"));
    }

    /**
     * 编译节点
     * <p>
     * 将 DSL 节点规范转换为编译后的节点，设置节点 ID、类型、配置和操作。
     * 
     * @param source DSL 节点规范
     * @return 编译后的节点
     */
    private CompiledNode compileNode(DslNodeSpec source) {
        CompiledNode node = new CompiledNode();
        node.setId(source.getId());
        node.setType(source.getType());
        node.setConfig(source.getConfig());
        // 从配置中获取 action，如果不存在则使用节点类型名称
        node.setAction((String) source.getConfig().getOrDefault("action", source.getType().name()));
        return node;
    }
}
