package com.sipc115.helix.integration.workflow.node.agent;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface AiChatActivity {

    Object chat(String prompt, String model);
}
