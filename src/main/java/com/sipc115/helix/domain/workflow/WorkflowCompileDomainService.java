package com.sipc115.helix.domain.workflow;

import com.sipc115.helix.integration.workflow.spi.DslCompiler;
import com.sipc115.helix.repository.jpa.JpaExecutionPlanRepository;
import com.sipc115.helix.repository.jpa.JpaWorkflowDslRepository;
import com.sipc115.helix.service.WorkflowApplicationService;
import com.sipc115.helix.utils.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowCompileDomainService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCompileDomainService.class);

    private final DslCompiler dslCompiler;

    public WorkflowCompileDomainService(DslCompiler dslCompiler) {
        this.dslCompiler = dslCompiler;
    }

    public ExecutionPlan compile(WorkflowDsl dsl) {
        log.info("Compiling workflow DSL. workflowId={}, version={}",
                dsl.getWorkflowId(), dsl.getVersion());

        validateForCompilation(dsl);

        ExecutionPlan plan = dslCompiler.compile(dsl);

        log.info("Workflow compile successfully. planId={}, workflowId={}",
                plan.getPlanId(), plan.getWorkflowId());

        return plan;
    }

    public void validateForCompilation(WorkflowDsl dsl) {
        if (dsl == null) {
            throw new IllegalArgumentException("DSL cannot be null");
        }

        if (dsl.getWorkflowId() == null || dsl.getWorkflowId().isEmpty()) {
            throw new IllegalArgumentException("Workflow ID cannot be empty");
        }

        if (dsl.getVersion() == null || dsl.getVersion().isEmpty()) {
            throw new IllegalArgumentException("Workflow version cannot be empty");
        }

        if (dsl.getNodes() == null || dsl.getNodes().isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one node");
        }

        if (dsl.getEdges() == null || dsl.getEdges().isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one edge");
        }

        boolean hasStart = dsl.getNodes().stream()
                .anyMatch(node -> node.getType() == DslNodeType.START);
        boolean hasEnd = dsl.getNodes().stream()
                .anyMatch(node -> node.getType() == DslNodeType.END);

        if (!hasStart) {
            throw new IllegalArgumentException("Workflow must have a START node");
        }

        if (!hasEnd) {
            throw new IllegalArgumentException("Workflow must have an END node");
        }

        log.debug("DSL validation passed for workflow: {}", dsl.getWorkflowId());
    }
}
