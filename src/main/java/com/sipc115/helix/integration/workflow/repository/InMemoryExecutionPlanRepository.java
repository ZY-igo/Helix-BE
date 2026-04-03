/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.repository;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.integration.workflow.spi.ExecutionPlanRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryExecutionPlanRepository implements ExecutionPlanRepository {
    private final Map<String, ExecutionPlan> store = new ConcurrentHashMap<>();

    @Override
    public void save(ExecutionPlan plan) {
        store.put(key(plan.getWorkflowId(), plan.getWorkflowVersion()), plan);
    }

    @Override
    public Optional<ExecutionPlan> findByWorkflowIdAndVersion(String workflowId, String version) {
        return Optional.ofNullable(store.get(key(workflowId, version)));
    }

    private String key(String workflowId, String version) {
        return workflowId + ":" + version;
    }
}