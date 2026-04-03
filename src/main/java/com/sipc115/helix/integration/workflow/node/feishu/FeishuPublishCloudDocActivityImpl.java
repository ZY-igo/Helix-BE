package com.sipc115.helix.integration.workflow.node.feishu;

import com.sipc115.helix.integration.lark.FeishuClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeishuPublishCloudDocActivityImpl implements FeishuPublishCloudDocActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuPublishCloudDocActivityImpl.class);

    @Autowired
    private FeishuClient feishuClient;

    @Override
    public String publishCloudDoc(String title, String content) {
        try {
            log.info("Publishing Feishu cloud doc. title={}", title);

            String docUrl = feishuClient.publishCloudDocIfEnabled(title, content);

            if (docUrl != null) {
                log.info("Feishu cloud doc published. docUrl={}", docUrl);
            } else {
                log.warn("Feishu cloud doc is disabled, skipped publishing");
            }

            return docUrl;
        } catch (Exception e) {
            log.error("Failed to publish Feishu cloud doc", e);
            return null;
        }
    }
}
