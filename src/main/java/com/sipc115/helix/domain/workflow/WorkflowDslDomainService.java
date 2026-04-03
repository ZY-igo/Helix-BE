package com.sipc115.helix.domain.workflow;

import com.sipc115.helix.utils.WorkflowVersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkflowDslDomainService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowDslDomainService.class);

    public WorkflowDslDomainService() {
    }

    public static WorkflowDsl createNew(String name, String createdBy) {
        String workflowId = generateWorkflowId();
        String initialVersion = WorkflowVersionUtils.initialVersion();

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId(workflowId);
        dsl.setName(name != null ? name : "未命名工作流");
        dsl.setVersion(initialVersion);

        WorkflowMetadata metadata = new WorkflowMetadata();
        metadata.setScope(WorkflowMetadata.WorkflowScope.PRIVATE);
        metadata.setState(WorkflowMetadata.WorkflowState.DRAFT);
        metadata.setCreatedBy(createdBy);
        metadata.setUpdatedBy(createdBy);
        dsl.setMetadata(metadata);

        dsl.setNodes(createInitialNodes());
        dsl.setEdges(createInitialEdges());

        log.info("Created new workflow DSL. workflowId={}, version={}", workflowId, initialVersion);
        return dsl;
    }

    public static String generateWorkflowId() {
        return "wf_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static List<DslNodeSpec> createInitialNodes() {
        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec startNode = new DslNodeSpec();
        startNode.setId("start");
        startNode.setType(DslNodeType.START);
        startNode.setName("开始");
        startNode.setCategory(NodeCategory.CONTROL_FLOW);
        nodes.add(startNode);

        DslNodeSpec endNode = new DslNodeSpec();
        endNode.setId("end");
        endNode.setType(DslNodeType.END);
        endNode.setName("结束");
        endNode.setCategory(NodeCategory.CONTROL_FLOW);
        nodes.add(endNode);

        return nodes;
    }

    public static List<DslEdgeSpec> createInitialEdges() {
        List<DslEdgeSpec> edges = new ArrayList<>();

        DslEdgeSpec edge = new DslEdgeSpec();
        edge.setFrom("start");
        edge.setTo("end");
        edges.add(edge);

        return edges;
    }

    public static WorkflowDsl copyAsNewVersion(WorkflowDsl source, String newVersion) {
        WorkflowDsl copy = new WorkflowDsl();
        copy.setWorkflowId(source.getWorkflowId());
        copy.setName(source.getName());
        copy.setVersion(newVersion);

        WorkflowMetadata metadata = new WorkflowMetadata();
        metadata.setScope(source.getMetadata() != null
                ? source.getMetadata().getScope()
                : WorkflowMetadata.WorkflowScope.PRIVATE);
        metadata.setState(WorkflowMetadata.WorkflowState.DRAFT);
        metadata.setCreatedBy(source.getMetadata() != null
                ? source.getMetadata().getCreatedBy()
                : "system");
        metadata.setUpdatedBy("system");
        copy.setMetadata(metadata);

        copy.setNodes(deepCopyNodes(source.getNodes()));
        copy.setEdges(deepCopyEdges(source.getEdges()));
        copy.setSchedule(source.getSchedule());

        return copy;
    }

    public static WorkflowDsl copyAsNewWorkflow(WorkflowDsl source, String newWorkflowId, String newVersion) {
        WorkflowDsl copy = new WorkflowDsl();
        copy.setWorkflowId(newWorkflowId);
        copy.setName(source.getName() + " (副本)");
        copy.setVersion(newVersion);

        WorkflowMetadata metadata = new WorkflowMetadata();
        metadata.setScope(source.getMetadata() != null
                ? source.getMetadata().getScope()
                : WorkflowMetadata.WorkflowScope.PRIVATE);
        metadata.setState(WorkflowMetadata.WorkflowState.DRAFT);
        metadata.setCreatedBy("system");
        metadata.setUpdatedBy("system");
        copy.setMetadata(metadata);

        copy.setNodes(deepCopyNodes(source.getNodes()));
        copy.setEdges(deepCopyEdges(source.getEdges()));
        copy.setSchedule(source.getSchedule());

        return copy;
    }

    public static List<DslNodeSpec> deepCopyNodes(List<DslNodeSpec> original) {
        if (original == null) {
            return new ArrayList<>();
        }

        List<DslNodeSpec> copy = new ArrayList<>();
        for (DslNodeSpec node : original) {
            DslNodeSpec nodeCopy = new DslNodeSpec();
            nodeCopy.setId(node.getId());
            nodeCopy.setType(node.getType());
            nodeCopy.setName(node.getName());
            nodeCopy.setCategory(node.getCategory());
            nodeCopy.setPolicy(node.getPolicy() != null ? deepCopyPolicy(node.getPolicy()) : null);
            nodeCopy.setConfig(node.getConfig() != null ? new java.util.HashMap<>(node.getConfig()) : null);
            copy.add(nodeCopy);
        }
        return copy;
    }

    public static List<DslEdgeSpec> deepCopyEdges(List<DslEdgeSpec> original) {
        if (original == null) {
            return new ArrayList<>();
        }

        List<DslEdgeSpec> copy = new ArrayList<>();
        for (DslEdgeSpec edge : original) {
            DslEdgeSpec edgeCopy = new DslEdgeSpec();
        edgeCopy.setFrom(edge.getFrom());
        edgeCopy.setTo(edge.getTo());
        edgeCopy.setConditionKey(edge.getConditionKey());
            copy.add(edgeCopy);
        }
        return copy;
    }

    public static NodePolicyConfig deepCopyPolicy(NodePolicyConfig original) {
        if (original == null) {
            return null;
        }

        NodePolicyConfig copy = new NodePolicyConfig();

        if (original.getRetryPolicy() != null) {
            NodePolicyConfig.RetryPolicy retryCopy = new NodePolicyConfig.RetryPolicy();
            retryCopy.setMaxAttempts(original.getRetryPolicy().getMaxAttempts());
            retryCopy.setInitialInterval(original.getRetryPolicy().getInitialInterval());
            retryCopy.setMaxInterval(original.getRetryPolicy().getMaxInterval());
            retryCopy.setBackoffCoefficient(original.getRetryPolicy().getBackoffCoefficient());
            retryCopy.setRetryableExceptions(new ArrayList<>(original.getRetryPolicy().getRetryableExceptions()));
            retryCopy.setNonRetryableExceptions(new ArrayList<>(original.getRetryPolicy().getNonRetryableExceptions()));
            copy.setRetryPolicy(retryCopy);
        }

        if (original.getTimeout() != null) {
            NodePolicyConfig.TimeoutConfig timeoutCopy = new NodePolicyConfig.TimeoutConfig();
            timeoutCopy.setExecutionTimeout(original.getTimeout().getExecutionTimeout());
            timeoutCopy.setScheduleToCloseTimeout(original.getTimeout().getScheduleToCloseTimeout());
            timeoutCopy.setScheduleToStartTimeout(original.getTimeout().getScheduleToStartTimeout());
            timeoutCopy.setStartToCloseTimeout(original.getTimeout().getStartToCloseTimeout());
            copy.setTimeout(timeoutCopy);
        }

        return copy;
    }
}
