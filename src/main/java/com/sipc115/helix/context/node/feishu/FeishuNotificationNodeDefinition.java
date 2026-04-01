/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.feishu;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 飞书通知节点定义
 * <p>
 * 定义飞书通知节点（FEISHU_NOTIFICATION）的元数据和配置类型。
 * <p>
 * 配置结构：
 * <pre>
 * {
 *   "action": "sendText|sendPost|sendPostWithLink|publishCloudDoc",
 *   "chatId": "chat_xxx",
 *   "text": "消息内容",
 *   "title": "标题",
 *   "lines": ["行1", "行2"],
 *   "url": "https://xxx",
 *   "linkText": "链接文本",
 *   "content": "文档内容",
 *   "activityConfig": {
 *     "timeout": "PT2M",
 *     "taskQueue": "urgent-queue",
 *     "retry": {
 *       "initialInterval": "PT1S",
 *       "maxInterval": "PT1M",
 *       "maxAttempts": 3
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuNotificationNodeDefinition implements NodeDefinition<Map<String, Object>> {

    /**
     * 获取节点类型
     *
     * @return FEISHU_NOTIFICATION 节点类型枚举
     */
    @Override
    public DslNodeType type() {
        return DslNodeType.FEISHU_NOTIFICATION;
    }

    /**
     * 获取配置类
     * <p>
     * 飞书通知节点使用 Map 作为配置类型，支持灵活的配置结构。
     * 实际配置验证由 {@link FeishuNotificationNodeCompiler} 负责。
     *
     * @return Map 配置类
     */
    @Override
    public Class<Map<String, Object>> configClass() {
        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> clazz = (Class<Map<String, Object>>) (Class<?>) Map.class;
        return clazz;
    }
}
