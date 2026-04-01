/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.spi;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.WorkflowDsl;

/**
 * DSL 编译器接口
 * <p>
 * 定义工作流 DSL 到执行计划的编译操作，是 DSL 编译的抽象接口。
 * 实现类负责具体的编译逻辑，将 DSL 转换为可执行的执行计划。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public interface DslCompiler {
    /**
     * 编译工作流 DSL
     * <p>
     * 将工作流 DSL 编译为执行计划，包含工作流的节点、边和执行逻辑。
     * 
     * @param dsl 工作流 DSL 对象
     * @return 编译后的执行计划
     */
    ExecutionPlan compile(WorkflowDsl dsl);
}
