/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

/**
 * 飞书发布云文档节点定义
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuPublishCloudDocNodeDefinition implements NodeDefinition<FeishuPublishCloudDocConfig> {

    @Override
    public DslNodeType type() {
        return DslNodeType.FEISHU_PUBLISH_CLOUD_DOC;
    }

    @Override
    public Class<FeishuPublishCloudDocConfig> configClass() {
        return FeishuPublishCloudDocConfig.class;
    }
}
