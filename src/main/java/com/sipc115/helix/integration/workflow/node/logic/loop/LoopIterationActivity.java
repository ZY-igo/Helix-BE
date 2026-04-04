package com.sipc115.helix.integration.workflow.node.logic.loop;

import io.temporal.activity.ActivityInterface;

import java.util.Map;

@ActivityInterface
public interface LoopIterationActivity {

    Object execute(Map<String, Object> config, Integer iteration);
}
