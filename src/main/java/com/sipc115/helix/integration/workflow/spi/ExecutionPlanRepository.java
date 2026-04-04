/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.spi;

import com.sipc115.helix.domain.workflow.ExecutionPlan;

import java.util.List;
import java.util.Optional;

/**
 * 执行计划仓库接口
 * <p>
 * 定义执行计划（ExecutionPlan）的存储和查询操作。
 * 采用策略模式，支持内存存储和数据库持久化等多种实现。
 *
 * @author Helix Team
 * @since 2.0.0
 */
public interface ExecutionPlanRepository {

    /**
     * 保存执行计划
     * <p>
     * 如果已存在相同 workflowId:version 的计划，则覆盖更新。
     *
     * @param plan 执行计划
     */
    void save(ExecutionPlan plan);

    /**
     * 根据工作流ID和版本查询执行计划
     *
     * @param workflowId 工作流ID
     * @param version 工作流版本
     * @return 执行计划，如果不存在则返回空
     */
    Optional<ExecutionPlan> findByWorkflowIdAndVersion(String workflowId, String version);

    /**
     * 根据工作流ID查询所有版本
     *
     * @param workflowId 工作流ID
     * @return 执行计划列表
     */
    List<ExecutionPlan> findByWorkflowId(String workflowId);

    /**
     * 删除指定版本的执行计划
     *
     * @param workflowId 工作流ID
     * @param version 工作流版本
     */
    void deleteByWorkflowIdAndVersion(String workflowId, String version);

    /**
     * 查询所有执行计划
     *
     * @return 所有执行计划列表
     */
    List<ExecutionPlan> findAll();
}
