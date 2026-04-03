package com.sipc115.helix.integration.workflow.node.feishu;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface FeishuPublishCloudDocActivity {

    String publishCloudDoc(String title, String content);
}
