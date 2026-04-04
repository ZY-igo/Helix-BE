package com.sipc115.helix.integration.workflow.node.logic.loop;

import lombok.Data;

/**
 * Loop Node Configuration
 *
 * <p>Defines the configuration for a loop node that executes a sub-workflow repeatedly.
 *
 * <p>Exit conditions:
 * <ul>
 *   <li>Condition met - when the exit condition expression evaluates to true</li>
 *   <li>Max iterations reached - when iteration count exceeds maxRounds</li>
 * </ul>
 */
@Data
public class LoopNodeConfig {

    /**
     * Exit condition expression
     * <p>
     * An Aviator expression that determines when to exit the loop.
     * When this expression evaluates to true, the loop exits.
     *
     * <p>Example:
     * <pre>
     * "iteration >= 10"
     * "result.status == 'completed'"
     * "!hasMoreData"
     * </pre>
     */
    private String exitCondition;

    /**
     * Maximum number of iterations
     * <p>
     * Protection against infinite loops.
     * When this count is reached, the loop exits regardless of condition.
     *
     * <p>Default: 10
     */
    private Integer maxRounds = 10;

    /**
     * Sub-workflow DSL
     * <p>
     * The DSL definition of the sub-workflow to execute in each iteration.
     * This is a nested WorkflowDsl object containing nodes and edges.
     */
    private Object subWorkflowDsl;

    /**
     * Loop variable name
     * <p>
     * The name of the variable that tracks the current iteration count.
     * This variable is available in the exit condition expression.
     *
     * <p>Default: "iteration"
     */
    private String loopVariable = "iteration";

    /**
     * Result variable name
     * <p>
     * The name of the variable that stores the result of each iteration.
     * After each iteration, the result is stored in this variable.
     *
     * <p>Default: "loopResult"
     */
    private String resultVariable = "loopResult";
}
