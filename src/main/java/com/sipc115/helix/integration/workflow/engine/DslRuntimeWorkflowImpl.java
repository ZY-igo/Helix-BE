/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.engine;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.domain.workflow.WorkflowStateView;
import com.sipc115.helix.integration.workflow.runtime.DslOrchestratorWorkflowImpl;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * DSL 杩愯鏃跺伐浣滄祦瀹炵幇绫?
 * <p>
 * 鍩轰簬 Temporal 妗嗘灦瀹炵幇鐨勫伐浣滄祦杩愯鏃讹紝璐熻矗鎵ц DSL 瀹氫箟鐨勫伐浣滄祦銆?
 * 鎻愪緵宸ヤ綔娴佺殑鍚姩銆佷俊鍙峰鐞嗗拰鐘舵€佹煡璇㈠姛鑳姐€?
 */
public class DslRuntimeWorkflowImpl implements DslRuntimeWorkflow {

    private static final Logger logger = LoggerFactory.getLogger(DslRuntimeWorkflowImpl.class);
    private static volatile WorkflowTraceService traceService;

    // 瑙ｉ噴鍣ㄥ疄渚嬶紙涓嶆槸 Spring Bean锛?
    private final DslOrchestratorWorkflowImpl interpreter = new DslOrchestratorWorkflowImpl();

    public static void setTraceService(WorkflowTraceService traceService) {
        DslRuntimeWorkflowImpl.traceService = traceService;
    }

    public DslRuntimeWorkflowImpl() {
        // Temporal 鏃犲弬鏋勯€犲嚱鏁?
    }

    @Override
    public void run(ExecutionPlan plan, Map<String, Object> input) {
        logger.info("Starting workflow execution. workflowId={}, version={}",
                   plan.getWorkflowId(), plan.getWorkflowVersion());

        Long executionId = extractExecutionId(input);

        // 浣跨敤 Temporal 鐨?CancellationScope 鏉ュ鐞嗗伐浣滄祦鍙栨秷
        CancellationScope cancellationScope = Workflow.newCancellationScope(() -> {
            try {
                // 濮旀墭缁欒В閲婂櫒鎵ц
                interpreter.run(plan, input);
                if (traceService != null && executionId != null) {
                    traceService.markExecutionSuccess(executionId, buildOutputSnapshot());
                }
                logger.info("Workflow execution completed successfully.");
            } catch (Exception e) {
                if (traceService != null && executionId != null) {
                    traceService.markExecutionFailed(executionId, e.getMessage());
                }
                logger.error("Workflow execution failed: {}", e.getMessage(), e);
                throw e;
            }
        });

        // 鍚姩鍙栨秷浣滅敤鍩熷苟绛夊緟瀹屾垚
        cancellationScope.run();
    }

    @Override
    public void provideHumanInput(HumanSignalPayload payload) {
        logger.info("Received human input signal for node: {}", payload.getNodeId());
        interpreter.provideHumanInput(payload);
    }

    @Override
    public WorkflowStateView currentState() {
        return interpreter.currentState();
    }

    private Long extractExecutionId(Map<String, Object> input) {
        if (input == null) {
            return null;
        }

        Object rawExecutionId = input.get("_executionId");
        if (rawExecutionId instanceof Long) {
            return (Long) rawExecutionId;
        }
        if (rawExecutionId instanceof Integer) {
            return ((Integer) rawExecutionId).longValue();
        }
        if (rawExecutionId instanceof String rawText) {
            try {
                return Long.parseLong(rawText);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> buildOutputSnapshot() {
        Map<String, Object> variables = new HashMap<>(interpreter.currentState().getVariables());
        variables.remove("_executionId");
        return variables;
    }
}
