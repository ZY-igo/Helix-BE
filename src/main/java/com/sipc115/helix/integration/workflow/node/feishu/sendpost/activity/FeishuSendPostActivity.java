/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost.activity;

import io.temporal.activity.ActivityInterface;

import java.util.List;

/**
 * 飞书发送富文本消息 Activity 接口
 * <p>
 * 定义了向飞书发送富文本（Post）消息的 Activity 方法。
 * 使用 Temporal Activity 将飞书 API 调用从 Workflow 线程分离出来，
 * 保证 Workflow 的确定性执行。
 *
 * <h3>富文本消息结构：</h3>
 * <pre>
 * Post 消息包含：
 * - title: 标题
 * - lines: 内容行列表，每行可以是文本或链接
 * </pre>
 *
 * <h3>设计目的：</h3>
 * <ul>
 *   <li>将非确定性操作（HTTP 调用）从 Workflow 中分离</li>
 *   <li>支持 Temporal 的重试和错误处理机制</li>
 *   <li>保证 Workflow 重放时的一致性</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuSendPostActivityImpl
 * @see io.temporal.activity.ActivityInterface
 */
@ActivityInterface
public interface FeishuSendPostActivity {

    /**
     * 发送飞书富文本消息
     * <p>
     * 通过飞书 IM API 向指定会话发送富文本消息。
     *
     * <h3>主要流程：</h3>
     * <ol>
     *   <li>获取飞书访问令牌（通过 FeishuAuthClient）</li>
     *   <li>调用飞书发送消息 API</li>
     *   <li>解析响应中的 message_id</li>
     * </ol>
     *
     * <h3>错误处理：</h3>
     * <ul>
     *   <li>网络错误：Temporal 会根据配置的重试策略自动重试</li>
     *   <li>业务错误（如 token 过期）：抛出异常，由调用方处理</li>
     *   <li>API 返回错误码：抛出 IllegalStateException</li>
     * </ul>
     *
     * @param connectionId 飞书连接配置 ID
     * @param chatId 接收消息的会话 ID
     * @param title 消息标题
     * @param lines 内容行列表
     * @return 发送成功后的消息 ID（message_id）
     * @throws IllegalArgumentException 如果 connectionId 为 null 或 chatId 为空
     * @throws IllegalStateException 如果 API 调用失败
     */
    String sendPost(Long connectionId, String chatId, String title, List<String> lines);

    /**
     * 发送飞书富文本消息（使用连接配置）
     * <p>
     * 重载方法，允许直接传入连接配置而非 connectionId。
     *
     * @param connectionConfig 连接配置对象
     * @param chatId 接收消息的会话 ID
     * @param title 消息标题
     * @param lines 内容行列表
     * @return 发送成功后的消息 ID
     */
    String sendPostWithConfig(Object connectionConfig, String chatId, String title, List<String> lines);
}