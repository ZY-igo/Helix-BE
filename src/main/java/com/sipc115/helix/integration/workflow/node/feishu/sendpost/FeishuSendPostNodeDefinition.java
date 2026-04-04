/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

/**
 * 飞书发送富文本消息节点定义
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuSendPostNodeDefinition implements NodeDefinition<FeishuSendPostConfig> {

    @Override
    public DslNodeType type() {
        return DslNodeType.FEISHU_SEND_POST;
    }

    @Override
    public Class<FeishuSendPostConfig> configClass() {
        return FeishuSendPostConfig.class;
    }
}
