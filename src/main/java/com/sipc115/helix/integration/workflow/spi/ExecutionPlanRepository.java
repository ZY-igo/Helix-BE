/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.spi;

import com.sipc115.helix.domain.workflow.ExecutionPlan;

import java.util.Optional;

public interface ExecutionPlanRepository {
    void save(ExecutionPlan plan);

    Optional<ExecutionPlan> findByWorkflowIdAndVersion(String workflowId, String version);
}