/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.spi;

import com.sipc115.helix.domain.workflow.WorkflowDsl;

import java.util.List;
import java.util.Optional;

public interface DslRepository {
    void save(WorkflowDsl dsl);

    Optional<WorkflowDsl> findByWorkflowIdAndVersion(String workflowId, String version);

    List<WorkflowDsl> findAllVersions(String workflowId);

    Optional<WorkflowDsl> findLatestPublished(String workflowId);
}
