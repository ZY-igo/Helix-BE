package com.sipc115.helix.integration.workflow.node.logic.loop;

import com.sipc115.helix.integration.expression.CompiledExpression;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;

public class LoopSubWorkflowImpl implements LoopSubWorkflow {

    private static final Logger log = LoggerFactory.getLogger(LoopSubWorkflowImpl.class);

    @Override
    public LoopResult execute(Integer maxRounds, Map<String, Object> input) {
        log.info("Starting loop sub-workflow with maxRounds={}", maxRounds);

        int iteration = 0;
        Object lastResult = null;
        String exitReason = "maxIterations";

        while (iteration < maxRounds) {
            iteration++;

            log.info("Loop iteration {} started", iteration);

            ActivityOptions activityOptions = ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .build())
                    .build();

            LoopIterationActivity activity = Workflow.newActivityStub(
                    LoopIterationActivity.class,
                    activityOptions
            );

            try {
                lastResult = activity.execute(input, iteration);
                log.info("Loop iteration {} completed with result: {}", iteration, lastResult);
            } catch (Exception e) {
                log.error("Loop iteration {} failed: {}", iteration, e.getMessage());
                lastResult = Map.of("error", e.getMessage(), "iteration", iteration);
            }

            input.put("lastResult", lastResult);

            if (shouldExit(input, lastResult)) {
                exitReason = "conditionMet";
                log.info("Loop exiting due to condition met at iteration {}", iteration);
                break;
            }
        }

        if (iteration >= maxRounds) {
            log.info("Loop reached max iterations: {}", maxRounds);
        }

        return new LoopResult(iteration, lastResult, exitReason);
    }

    private boolean shouldExit(Map<String, Object> input, Object lastResult) {
        Object exitConditionObj = input.get("exitCondition");
        if (exitConditionObj == null) {
            return false;
        }

        if (exitConditionObj instanceof CompiledExpression) {
            try {
                CompiledExpression compiledExpr = (CompiledExpression) exitConditionObj;
                Boolean result = (Boolean) compiledExpr.execute(input);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.warn("Failed to evaluate exit condition: {}", e.getMessage());
                return false;
            }
        }

        return false;
    }
}
