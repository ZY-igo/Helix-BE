/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext.activity;

import io.temporal.activity.ActivityInterface;

import java.util.Map;

/**
 * 飞书发送文本消息 Activity 接口
 * <p>
 * 定义了向飞书发送文本消息的 Activity 方法。
 * 使用 Temporal Activity 将飞书 API 调用从 Workflow 线程分离出来，
 * 保证 Workflow 的确定性执行。
 *
 * <h3>设计目的：</h3>
 * <ul>
 *   <li>将非确定性操作（HTTP 调用）从 Workflow 中分离</li>
 *   <li>支持 Temporal 的重试和错误处理机制</li>
 *   <li>保证 Workflow 重放时的一致性</li>
 * </ul>
 *
 * <h3>Activity 调用方式：</h3>
 * <pre>
 * {@code
 * // 在 NodeExecutor 中通过 bridge 获取 Activity 存根
 * FeishuSendTextActivity activity = bridge.activities()
 *     .getActivity(FeishuSendTextActivity.class);
 *
 * // 调用 Activity 方法
 * String messageId = activity.sendText(connectionId, chatId, text);
 * }
 * </pre>
 *
 * <h3>Temporal 执行流程：</h3>
 * <pre>
 * Temporal Workflow Thread
 *   └─ FeishuSendTextNodeExecutor.execute()
 *       └─ bridge.activities().getActivity(FeishuSendTextActivity.class)
 *           └─ FeishuSendTextActivity.sendText()  ← 在 Activity Worker 上执行
 *               └─ FeishuApiHandler.sendText()   ← 真正的 HTTP 调用
 *                   └─ RestClient.post()          ← 非确定性操作
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuSendTextActivityImpl
 * @see io.temporal.activity.ActivityInterface
 */
@ActivityInterface
public interface FeishuSendTextActivity {

    /**
     * 发送飞书文本消息
     * <p>
     * 通过飞书 IM API 向指定会话发送文本消息。
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
     * <h3>幂等性：</h3>
     * <p>
     * 此方法本身不是幂等的。
     * 如果需要幂等发送，应在调用方使用 messageId 进行去重。
     *
     * @param connectionId 飞书连接配置 ID
     * @param chatId 接收消息的会话 ID
     * @param text 消息内容
     * @return 发送成功后的消息 ID（message_id）
     * @throws IllegalArgumentException 如果 connectionId 为 null 或 chatId 为空
     * @throws IllegalStateException 如果 API 调用失败
     */
    String sendText(Long connectionId, String chatId, String text);

    /**
     * 发送飞书文本消息（使用连接配置）
     * <p>
     * 重载方法，允许直接传入连接配置而非 connectionId。
     * 用于在已获取连接配置的场景下避免重复查询。
     *
     * @param connectionConfig 连接配置（来自 ConnectionClientRegistry）
     * @param chatId 接收消息的会话 ID
     * @param text 消息内容
     * @return 发送成功后的消息 ID
     */
    String sendTextWithConfig(Object connectionConfig, String chatId, String text);
}