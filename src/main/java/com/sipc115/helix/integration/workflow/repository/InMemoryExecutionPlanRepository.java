/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.repository;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.integration.workflow.port.ExecutionPlanRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存中的执行计划存储库实现
 * <p>
 * 使用内存中的 ConcurrentHashMap 存储执行计划，提供保存和查询功能。
 * 适用于开发和测试环境，不适合生产环境使用。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Repository
public class InMemoryExecutionPlanRepository implements ExecutionPlanRepository {
    /**
     * 存储执行计划的内存映射
     * <p>
     * 使用 ConcurrentHashMap 确保线程安全，键为 workflowId:version 格式。
     */
    private final Map<String, ExecutionPlan> store = new ConcurrentHashMap<>();

    /**
     * 保存执行计划
     * <p>
     * 将执行计划存储到内存映射中，使用 workflowId:version 作为键。
     * 
     * @param plan 执行计划对象
     */
    @Override
    public void save(ExecutionPlan plan) {
        // 使用 key 方法生成存储键，然后将执行计划存储到映射中
        store.put(key(plan.getWorkflowId(), plan.getWorkflowVersion()), plan);
    }

    /**
     * 根据工作流 ID 和版本查询执行计划
     * <p>
     * 从内存映射中查询指定工作流 ID 和版本的执行计划。
     * 
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 包含执行计划的 Optional 对象，如果不存在则返回 Optional.empty()
     */
    @Override
    public Optional<ExecutionPlan> findByWorkflowIdAndVersion(String workflowId, Integer version) {
        // 使用 key 方法生成查询键，然后从映射中获取执行计划
        return Optional.ofNullable(store.get(key(workflowId, version)));
    }

    /**
     * 生成存储键
     * <p>
     * 根据工作流 ID 和版本生成唯一的存储键，格式为 workflowId:version。
     * 
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 生成的存储键
     */
    private String key(String workflowId, Integer version) {
        return workflowId + ":" + version;
    }
}
