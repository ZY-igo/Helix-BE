package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface FeishuPublishCloudDocActivity {

    String publishCloudDoc(Long connectionId, String title, String content);
}