/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

/**
 * 飞书发送带链接富文本消息节点定义
 * <p>
 * 定义飞书发送带链接富文本消息节点（FEISHU_SEND_POST_WITH_LINK）的元数据。
 *
 * <h3>DSL 配置示例：</h3>
 * <pre>
 * {
 *   "id": "sendPostWithLink",
 *   "type": "FEISHU_SEND_POST_WITH_LINK",
 *   "name": "发送带链接的飞书通知",
 *   "category": "NOTIFICATION",
 *   "config": {
 *     "connectionId": 123,
 *     "chatId": "oc_xxx",
 *     "title": "工作流完成通知",
 *     "text": "您的工作流已执行完成，点击查看详情",
 *     "url": "https://example.com/report/123",
 *     "linkText": "查看报告"
 *   }
 * }
 * </pre>
 *
 * <h3>配置参数：</h3>
 * <ul>
 *   <li>connectionId - 飞书连接 ID（必填，指向 FEISHU 类型的集成连接）</li>
 *   <li>chatId - 飞书群聊或用户 ID（必填）</li>
 *   <li>title - 消息标题（必填）</li>
 *   <li>text - 消息文本内容（必填）</li>
 *   <li>url - 链接地址（必填）</li>
 *   <li>linkText - 链接显示文本（可选，默认"查看文档"）</li>
 * </ul>
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
