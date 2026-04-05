/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;


/**
 * 飞书发送富文本消息节点定义
 * <p>
 * 定义飞书发送富文本消息节点（FEISHU_SEND_POST）的元数据。
 *
 * <h3>DSL 配置示例：</h3>
 * <pre>
 * {
 *   "id": "sendPostNotify",
 *   "type": "FEISHU_SEND_POST",
 *   "name": "发送飞书富文本",
 *   "category": "NOTIFICATION",
 *   "config": {
 *     "connectionId": 123,
 *     "chatId": "oc_xxx",
 *     "title": "工作流日报",
 *     "lines": [
 *       "今日完成：3 个任务",
 *       "待办事项：2 个"
 *     ]
 *   }
 * }
 * </pre>
 *
 * <h3>配置参数：</h3>
 * <ul>
 *   <li>connectionId - 飞书连接 ID（必填，指向 FEISHU 类型的集成连接）</li>
 *   <li>chatId - 飞书群聊或用户 ID（必填）</li>
 *   <li>title - 消息标题（必填）</li>
 *   <li>lines - 消息内容行列表（必填，List&lt;String&gt;）</li>
 * </ul>
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
