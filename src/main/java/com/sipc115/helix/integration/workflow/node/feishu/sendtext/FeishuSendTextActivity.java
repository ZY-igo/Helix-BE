/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface FeishuSendTextActivity {

    boolean sendText(Long connectionId, String chatId, String text);
}