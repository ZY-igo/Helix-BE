package com.sipc115.helix.integration.workflow.node.feishu;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface FeishuSendPostWithLinkActivity {

    boolean sendPostWithLink(String chatId, String title, String text, String url, String linkText);
}
