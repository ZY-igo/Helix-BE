/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.repository;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.integration.workflow.spi.ExecutionPlanRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存执行计划仓库
 * <p>
 * 使用内存 Map 存储执行计划，适合开发测试环境。
 * 生产环境应使用数据库持久化实现。
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Repository
public class InMemoryExecutionPlanRepository implements ExecutionPlanRepository {

    /**
     * 执行计划存储
     * <p>
     * Key 格式为 "workflowId:version"
     */
    private final Map<String, ExecutionPlan> store = new ConcurrentHashMap<>();

    @Override
    public void save(ExecutionPlan plan) {
        store.put(key(plan.getWorkflowId(), plan.getWorkflowVersion()), plan);
    }

    @Override
    public Optional<ExecutionPlan> findByWorkflowIdAndVersion(String workflowId, String version) {
        return Optional.ofNullable(store.get(key(workflowId, version)));
    }

    @Override
    public List<ExecutionPlan> findByWorkflowId(String workflowId) {
        return store.values().stream()
                .filter(plan -> plan.getWorkflowId().equals(workflowId))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByWorkflowIdAndVersion(String workflowId, String version) {
        store.remove(key(workflowId, version));
    }

    @Override
    public List<ExecutionPlan> findAll() {
        return new ArrayList<>(store.values());
    }

    private String key(String workflowId, String version) {
        return workflowId + ":" + version;
    }
}
