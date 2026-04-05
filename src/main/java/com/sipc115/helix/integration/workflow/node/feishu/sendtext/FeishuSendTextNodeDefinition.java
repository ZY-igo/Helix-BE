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
 * <h3>DSL 配置示例：</h3>
 * <pre>
 * {
 *   "id": "sendNotify",
 *   "type": "FEISHU_SEND_TEXT",
 *   "name": "发送飞书通知",
 *   "category": "NOTIFICATION",
 *   "config": {
 *     "connectionId": 123,
 *     "chatId": "oc_xxx",
 *     "text": "工作流测试消息"
 *   }
 * }
 * </pre>
 *
 * <h3>配置参数：</h3>
 * <ul>
 *   <li>connectionId - 飞书连接 ID（必填，指向 FEISHU 类型的集成连接）</li>
 *   <li>chatId - 飞书群聊或用户 ID（必填）</li>
 *   <li>text - 消息文本内容（必填）</li>
 * </ul>
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
