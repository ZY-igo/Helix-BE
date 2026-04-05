package com.sipc115.helix.integration.workflow.node.agent.chat;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface AiChatActivity {

    Object chat(String prompt, String model);
}
