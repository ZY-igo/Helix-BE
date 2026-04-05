/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost;

import lombok.Data;
import java.util.List;

/**
 * 飞书发送富文本消息节点配置
 * <p>
 * 用于配置飞书富文本消息（Post 消息）发送节点的参数。
 * 支持发送包含多行文本和简单格式的消息内容。
 *
 * <h3>DSL 中的配置方式：</h3>
 * <pre>
 * {
 *   "type": "FEISHU_SEND_POST",
 *   "config": {
 *     "connectionId": 123,
 *     "chatId": "oc_xxxx",
 *     "title": "每日报告",
 *     "lines": ["第一行内容", "第二行内容", "第三行内容"]
 *   }
 * }
 * </pre>
 *
 * <h3>字段说明：</h3>
 * <ul>
 *   <li>connectionId - 飞书连接ID，指向 FEISHU 类型的集成连接</li>
 *   <li>chatId - 接收消息的会话ID</li>
 *   <li>title - 消息标题（可选）</li>
 *   <li>lines - 消息内容行列表，每行作为独立段落发送</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuSendPostNodeExecutor
 * @see FeishuSendPostNodeDefinition
 */
@Data
public class FeishuSendPostConfig {

    /**
     * 飞书连接 ID
     * <p>
     * 指向 IntegrationConnection 表中 type='FEISHU' 的记录。
     */
    private Long connectionId;

    /**
     * 飞书会话 ID
     * <p>
     * 接收消息的会话 ID，支持 chat_id 或 open_id。
     */
    private String chatId;

    /**
     * 消息标题
     * <p>
     * 富文本消息的标题，会显示在消息顶部。
     */
    private String title;

    /**
     * 消息内容行
     * <p>
     * 消息的正文内容，每行作为独立段落。
     * 支持表达式 ${variable.name}，运行时自动求值。
     */
    private List<String> lines;
}
