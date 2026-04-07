/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext.activity;

import io.temporal.activity.ActivityInterface;

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
 *   <li>实现幂等性保护，防止重复发送消息</li>
 * </ul>
 *
 * <h3>幂等性保护：</h3>
 * <p>
 * 所有 sendText 方法都包含 executionId 和 retryCount 参数，
 * 用于实现幂等性检查。实现类会通过 WorkflowTraceService 查询
 * 之前是否已经成功执行过相同的 attempt，如果已成功则直接返回缓存结果。
 *
 * <h3>Activity 调用方式：</h3>
 * <pre>
 * {@code
 * // 在 NodeExecutor 中通过 bridge 获取 Activity 存根
 * FeishuSendTextActivity activity = bridge.activities()
 *     .getActivity(FeishuSendTextActivity.class);
 *
 * // 调用 Activity 方法（包含幂等参数）
 * String messageId = activity.sendText(
 *     executionId, retryCount, connectionId, chatId, text);
 * }
 * </pre>
 *
 * <h3>Temporal 执行流程：</h3>
 * <pre>
 * Temporal Workflow Thread
 *   └─ FeishuSendTextNodeExecutor.execute()
 *       └─ bridge.activities().getActivity(FeishuSendTextActivity.class)
 *           └─ FeishuSendTextActivity.sendText()  ← 在 Activity Worker 上执行
 *               └─ 幂等性检查
 *               └─ FeishuApiHandler.sendText()   ← 真正的 HTTP 调用
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
     * 实现了幂等性保护，防止 Temporal 重试导致重复发送。
     *
     * <h3>主要流程：</h3>
     * <ol>
     *   <li>检查该节点是否已成功执行过（通过 executionId + nodeId + retryCount）</li>
     *   <li>如果已成功，直接返回缓存的 messageId</li>
     *   <li>如果未成功，调用飞书发送消息 API</li>
     *   <li>解析响应中的 message_id</li>
     * </ol>
     *
     * <h3>幂等性保证：</h3>
     * <ul>
     *   <li>通过 attemptId = {executionId}_{nodeId}_{retryCount} 查询历史执行记录</li>
     *   <li>如果发现同 attemptId 的成功记录，直接返回缓存的 messageId</li>
     *   <li>避免重复调用飞书 API，防止用户收到重复消息</li>
     * </ul>
     *
     * @param executionId 工作流执行 ID（用于幂等键）
     * @param retryCount 当前重试次数（用于幂等键）
     * @param nodeId 节点 ID（用于幂等键）
     * @param connectionId 飞书连接配置 ID
     * @param chatId 接收消息的会话 ID
     * @param text 消息内容
     * @return 发送成功后的消息 ID（message_id）
     * @throws IllegalArgumentException 如果 connectionId 为 null 或 chatId 为空
     * @throws IllegalStateException 如果 API 调用失败
     */
    String sendText(Long executionId, Integer retryCount, String nodeId,
                    Long connectionId, String chatId, String text);

    /**
     * 发送飞书文本消息（使用连接配置）
     * <p>
     * 重载方法，允许直接传入连接配置而非 connectionId。
     * 用于在已获取连接配置的场景下避免重复查询。
     *
     * @param executionId 工作流执行 ID（用于幂等键）
     * @param retryCount 当前重试次数（用于幂等键）
     * @param nodeId 节点 ID（用于幂等键）
     * @param connectionConfig 连接配置对象
     * @param chatId 接收消息的会话 ID
     * @param text 消息内容
     * @return 发送成功后的消息 ID
     */
    String sendTextWithConfig(Long executionId, Integer retryCount, String nodeId,
                             Object connectionConfig, String chatId, String text);
}