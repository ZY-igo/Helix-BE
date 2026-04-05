package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc;

import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.lark.FeishuApiHandler;
import com.sipc115.helix.integration.connect.lark.FeishuAuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeishuPublishCloudDocActivityImpl implements FeishuPublishCloudDocActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuPublishCloudDocActivityImpl.class);

    @Autowired
    private ConnectionClientRegistry connectionRegistry;

    @Override
    @SuppressWarnings("unchecked")
    public String publishCloudDoc(Long connectionId, String title, String content) {
        try {
            log.info("Publishing Feishu cloud doc. title={}", title);

            FeishuAuthClient authClient = connectionRegistry.getOrCreateClient(connectionId, "FEISHU", null);
            String token = authClient.getToken();

            FeishuApiHandler handler = new FeishuApiHandler(
                    new com.fasterxml.jackson.databind.ObjectMapper(),
                    org.springframework.web.client.RestClient.builder()
            );

            String docId = handler.createDocument(token, title, null);

            if (docId != null) {
                log.info("Feishu cloud doc published. docId={}", docId);
            } else {
                log.warn("Feishu cloud doc is disabled, skipped publishing");
            }

            return docId;
        } catch (Exception e) {
            log.error("Failed to publish Feishu cloud doc", e);
            return null;
        }
    }
}