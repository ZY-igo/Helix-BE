/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.temporal.workflow;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.domain.workflow.WorkflowStateView;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Map;

/**
 * DSL 编排工作流接口
 * <p>
 * 定义 Temporal 工作流的核心方法，包括：
 * 1. 运行工作流实例
 * 2. 接收人工输入信号
 * 3. 查询工作流当前状态
 * <p>
 * 通过 Temporal 注解标记方法类型，实现工作流的执行、信号处理和状态查询。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@WorkflowInterface
public interface DslOrchestratorWorkflow {

    /**
     * 运行工作流
     * <p>
     * 启动并执行工作流实例，根据执行计划和输入参数处理工作流逻辑。
     * 
     * @param plan 执行计划，包含工作流的节点和边定义
     * @param input 输入参数，工作流执行所需的数据
     */
    @WorkflowMethod
    void run(ExecutionPlan plan, Map<String, Object> input);

    /**
     * 提供人工输入
     * <p>
     * 接收人工输入信号，用于处理需要人工干预的工作流节点。
     * 
     * @param payload 人工输入的有效载荷，包含节点 ID 和输入数据
     */
    @SignalMethod
    void provideHumanInput(HumanSignalPayload payload);

    /**
     * 查询当前状态
     * <p>
     * 获取工作流的当前执行状态，包括当前节点、执行状态、变量信息等。
     * 
     * @return 工作流状态视图，包含工作流的当前状态信息
     */
    @QueryMethod
    WorkflowStateView currentState();
}
