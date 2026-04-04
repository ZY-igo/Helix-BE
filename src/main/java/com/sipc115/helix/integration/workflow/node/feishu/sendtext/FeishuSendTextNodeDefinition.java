/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

/**
 * 飞书发送文本消息节点定义
 * <p>
 * 定义飞书发送文本消息节点（FEISHU_SEND_TEXT）的元数据。
 *
 * <h3>配置说明：</h3>
 * <pre>
 * {
 *   "chatId": "chat_xxx",
 *   "text": "消息内容"
 * }
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuSendTextNodeDefinition implements NodeDefinition<FeishuSendTextConfig> {

    @Override
    public DslNodeType type() {
        return DslNodeType.FEISHU_SEND_TEXT;
    }

    @Override
    public Class<FeishuSendTextConfig> configClass() {
        return FeishuSendTextConfig.class;
    }
}
