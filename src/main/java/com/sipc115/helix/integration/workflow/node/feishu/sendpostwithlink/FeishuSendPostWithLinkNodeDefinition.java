/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

/**
 * 飞书发送带链接富文本消息节点定义
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuSendPostWithLinkNodeDefinition implements NodeDefinition<FeishuSendPostWithLinkConfig> {

    @Override
    public DslNodeType type() {
        return DslNodeType.FEISHU_SEND_POST_WITH_LINK;
    }

    @Override
    public Class<FeishuSendPostWithLinkConfig> configClass() {
        return FeishuSendPostWithLinkConfig.class;
    }
}
