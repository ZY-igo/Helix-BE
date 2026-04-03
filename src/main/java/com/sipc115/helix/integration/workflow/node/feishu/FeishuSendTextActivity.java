package com.sipc115.helix.integration.workflow.node.feishu;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface FeishuSendTextActivity {

    boolean sendText(String chatId, String text);
}
