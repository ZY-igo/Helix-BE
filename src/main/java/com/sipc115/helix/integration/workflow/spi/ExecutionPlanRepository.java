/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.spi;

import com.sipc115.helix.domain.workflow.ExecutionPlan;

import java.util.Optional;

/**
 * 执行计划仓库接口
 * <p>
 * 定义执行计划的存储和查询操作，是执行计划持久化的抽象接口。
 * 实现类负责具体的存储逻辑，如内存存储、数据库存储等。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public interface ExecutionPlanRepository {
    /**
     * 保存执行计划
     * <p>
     * 将执行计划存储到仓库中，确保执行计划可以被后续查询和使用。
     * 
     * @param plan 执行计划对象
     */
    void save(ExecutionPlan plan);
    
    /**
     * 根据工作流 ID 和版本查询执行计划
     * <p>
     * 从仓库中查询指定工作流 ID 和版本的执行计划。
     * 
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 包含执行计划的 Optional 对象，如果不存在则返回 Optional.empty()
     */
    Optional<ExecutionPlan> findByWorkflowIdAndVersion(String workflowId, Integer version);
}
