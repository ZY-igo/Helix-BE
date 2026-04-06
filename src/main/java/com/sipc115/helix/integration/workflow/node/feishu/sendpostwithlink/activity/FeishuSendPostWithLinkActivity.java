/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink.activity;

import io.temporal.activity.ActivityInterface;

/**
 * 飞书发送带链接富文本消息 Activity 接口
 * <p>
 * 定义了向飞书发送带链接的富文本消息的 Activity 方法。
 * 使用 Temporal Activity 将飞书 API 调用从 Workflow 线程分离出来，
 * 保证 Workflow 的确定性执行。
 *
 * <h3>带链接富文本消息结构：</h3>
 * <pre>
 * Post 消息包含：
 * - title: 标题
 * - text: 前置文本内容
 * - url: 链接地址
 * - linkText: 链接显示文本
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuSendPostWithLinkActivityImpl
 * @see io.temporal.activity.ActivityInterface
 */
@ActivityInterface
public interface FeishuSendPostWithLinkActivity {

    /**
     * 发送飞书带链接富文本消息
     * <p>
     * 通过飞书 IM API 向指定会话发送带链接的富文本消息。
     *
     * @param connectionId 飞书连接配置 ID
     * @param chatId 接收消息的会话 ID
     * @param title 消息标题
     * @param text 前置文本内容
     * @param url 链接地址
     * @param linkText 链接显示文本
     * @throws IllegalArgumentException 如果参数无效
     * @throws IllegalStateException 如果 API 调用失败
     */
    void sendPostWithLink(Long connectionId, String chatId, String title, String text, String url, String linkText);

    /**
     * 发送飞书带链接富文本消息（使用连接配置）
     *
     * @param connectionConfig 连接配置对象
     * @param chatId 接收消息的会话 ID
     * @param title 消息标题
     * @param text 前置文本内容
     * @param url 链接地址
     * @param linkText 链接显示文本
     */
    void sendPostWithLinkWithConfig(Object connectionConfig, String chatId, String title, String text, String url, String linkText);
}