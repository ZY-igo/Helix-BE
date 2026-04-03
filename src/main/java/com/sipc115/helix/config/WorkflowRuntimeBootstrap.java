package com.sipc115.helix.config;

import com.sipc115.helix.integration.workflow.engine.DslRuntimeWorkflowImpl;
import com.sipc115.helix.integration.workflow.runtime.DslOrchestratorWorkflowImpl;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutorRegistry;
import com.sipc115.helix.integration.workflow.runtime.TransitionResolver;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class WorkflowRuntimeBootstrap {

    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final TransitionResolver transitionResolver;
    private final WorkflowTraceService workflowTraceService;

    public WorkflowRuntimeBootstrap(
            NodeExecutorRegistry nodeExecutorRegistry,
            TransitionResolver transitionResolver,
            WorkflowTraceService workflowTraceService
    ) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
        this.transitionResolver = transitionResolver;
        this.workflowTraceService = workflowTraceService;
    }

    @PostConstruct
    public void initialize() {
        DslOrchestratorWorkflowImpl.configureDefaults(nodeExecutorRegistry, transitionResolver);
        DslRuntimeWorkflowImpl.setTraceService(workflowTraceService);
    }
}
