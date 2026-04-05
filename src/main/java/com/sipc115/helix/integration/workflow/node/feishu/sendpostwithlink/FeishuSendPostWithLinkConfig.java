/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink;

import lombok.Data;

/**
 * 飞书发送带链接富文本消息节点配置
 * <p>
 * 用于配置飞书带链接的富文本消息发送节点的参数。
 * 支持在消息中添加超链接。
 *
 * <h3>DSL 中的配置方式：</h3>
 * <pre>
 * {
 *   "type": "FEISHU_SEND_POST_WITH_LINK",
 *   "config": {
 *     "connectionId": 123,
 *     "chatId": "oc_xxxx",
 *     "title": "报告链接",
 *     "text": "请点击查看详细报告",
 *     "url": "https://example.com/report",
 *     "linkText": "查看报告"
 *   }
 * }
 * </pre>
 *
 * <h3>字段说明：</h3>
 * <ul>
 *   <li>connectionId - 飞书连接ID，指向 FEISHU 类型的集成连接</li>
 *   <li>chatId - 接收消息的会话ID</li>
 *   <li>title - 消息标题</li>
 *   <li>text - 消息正文内容</li>
 *   <li>url - 点击链接跳转的 URL</li>
 *   <li>linkText - 链接显示文本</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuSendPostWithLinkNodeExecutor
 * @see FeishuSendPostWithLinkNodeDefinition
 */
@Data
public class FeishuSendPostWithLinkConfig {

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
     * 富文本消息的标题。
     */
    private String title;

    /**
     * 消息正文
     * <p>
     * 消息的正文内容，支持表达式 ${variable.name}。
     */
    private String text;

    /**
     * 链接 URL
     * <p>
     * 点击链接后跳转的目标地址。
     * 支持 http:// 和 https:// 协议。
     */
    private String url;

    /**
     * 链接显示文本
     * <p>
     * 链接在消息中显示的文本。
     * 例如：链接文本为"查看详情"，用户看到的是可点击的"查看详情"。
     */
    private String linkText;
}
