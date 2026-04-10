/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.humanInput;

import com.sipc115.helix.common.constant.SystemConfigConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class HumanInputNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(HumanInputNodeExecutor.class);

    @Override
    public boolean supports(String type) {
        return DslNodeType.HUMAN_INPUT.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        Map<String, Object> config = node.getConfig();
        Duration timeout = parseTimeout(config);

        log.info("等待人工输入，节点: {}, 超时时间: {}", node.getId(), timeout);

        HumanSignalPayload payload = bridge.awaitHumanSignal(node.getId(), timeout);

        NodeExecutionResult result = new NodeExecutionResult();

        if (payload == null) {
            log.warn("人工输入超时，节点: {}", node.getId());
            result.setStatus(ExecutionStatus.TIMED_OUT);
            result.setOutput(Map.of(
                "timeout", true,
                "nodeId", node.getId(),
                "message", "人工输入超时"
            ));
        } else {
            log.info("收到人工输入，节点: {}", node.getId());
            result.setStatus(ExecutionStatus.COMPLETED);
            result.setOutput(payload.getPayload());
        }

        return result;
    }

    private Duration parseTimeout(Map<String, Object> config) {
        Object timeoutValue = config.get("timeout");
        if (timeoutValue == null) {
            return Duration.ofHours(SystemConfigConstants.DEFAULT_TIMEOUT_HOURS);
        }

        if (timeoutValue instanceof Number) {
            long seconds = ((Number) timeoutValue).longValue();
            return Duration.ofSeconds(seconds);
        }

        if (timeoutValue instanceof String) {
            String str = ((String) timeoutValue).trim().toLowerCase();
            if (str.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(str.substring(0, str.length() - 1)));
            } else if (str.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(str.substring(0, str.length() - 1)));
            } else if (str.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(str.substring(0, str.length() - 1)));
            } else {
                return Duration.ofSeconds(Long.parseLong(str));
            }
        }

        return Duration.ofHours(SystemConfigConstants.DEFAULT_TIMEOUT_HOURS);
    }
}
