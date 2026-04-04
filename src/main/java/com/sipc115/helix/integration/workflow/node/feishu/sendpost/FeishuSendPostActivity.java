package com.sipc115.helix.integration.workflow.node.feishu.sendpost;

import io.temporal.activity.ActivityInterface;

import java.util.List;

@ActivityInterface
public interface FeishuSendPostActivity {

    boolean sendPost(String chatId, String title, List<String> lines);
}
