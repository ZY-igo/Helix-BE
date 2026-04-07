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
 * <h3>设计目的：</h3>
 * <ul>
 *   <li>将非确定性操作（HTTP 调用）从 Workflow 中分离</li>
 *   <li>支持 Temporal 的重试和错误处理机制</li>
 *   <li>保证 Workflow 重放时的一致性</li>
 *   <li>实现幂等性保护，防止重复发送消息</li>
 * </ul>
 *
 * <h3>幂等性保护：</h3>
 * <p>
 * 所有 sendPostWithLink 方法都包含 executionId、nodeId 和 retryCount 参数，
 * 用于实现幂等性检查。
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
     * 实现了幂等性保护。
     *
     * @param executionId 工作流执行 ID（用于幂等键）
     * @param retryCount 当前重试次数（用于幂等键）
     * @param nodeId 节点 ID（用于幂等键）
     * @param connectionId 飞书连接配置 ID
     * @param chatId 接收消息的会话 ID
     * @param title 消息标题
     * @param text 前置文本内容
     * @param url 链接地址
     * @param linkText 链接显示文本
     * @throws IllegalArgumentException 如果参数无效
     * @throws IllegalStateException 如果 API 调用失败
     */
    void sendPostWithLink(Long executionId, Integer retryCount, String nodeId,
                          Long connectionId, String chatId, String title, String text, String url, String linkText);

    /**
     * 发送飞书带链接富文本消息（使用连接配置）
     * <p>
     * 重载方法，允许直接传入连接配置而非 connectionId。
     * 实现了幂等性保护。
     *
     * @param executionId 工作流执行 ID（用于幂等键）
     * @param retryCount 当前重试次数（用于幂等键）
     * @param nodeId 节点 ID（用于幂等键）
     * @param connectionConfig 连接配置对象
     * @param chatId 接收消息的会话 ID
     * @param title 消息标题
     * @param text 前置文本内容
     * @param url 链接地址
     * @param linkText 链接显示文本
     */
    void sendPostWithLinkWithConfig(Long executionId, Integer retryCount, String nodeId,
                                   Object connectionConfig, String chatId, String title, String text, String url, String linkText);
}