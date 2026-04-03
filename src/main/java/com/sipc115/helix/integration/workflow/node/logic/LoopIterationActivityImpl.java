package com.sipc115.helix.integration.workflow.node.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LoopIterationActivityImpl implements LoopIterationActivity {

    private static final Logger log = LoggerFactory.getLogger(LoopIterationActivityImpl.class);

    @Override
    public Object execute(Map<String, Object> config, Integer iteration) {
        log.info("Executing loop iteration {} with config: {}", iteration, config);

        Object result = Map.of(
                "iteration", iteration,
                "status", "completed",
                "timestamp", System.currentTimeMillis()
        );

        log.info("Loop iteration {} completed", iteration);
        return result;
    }
}
