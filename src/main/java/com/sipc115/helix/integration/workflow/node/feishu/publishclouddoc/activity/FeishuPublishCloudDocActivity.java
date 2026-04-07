/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc.activity;

import io.temporal.activity.ActivityInterface;

/**
 * 飞书发布云文档 Activity 接口
 * <p>
 * 定义了向飞书云空间发布云文档的 Activity 方法。
 * 使用 Temporal Activity 将飞书 API 调用从 Workflow 线程分离出来，
 * 保证 Workflow 的确定性执行。
 *
 * <h3>云文档发布流程：</h3>
 * <ol>
 *   <li>获取飞书访问令牌</li>
 *   <li>调用创建云文档 API</li>
 *   <li>返回文档 ID 和访问 URL</li>
 * </ol>
 *
 * <h3>设计目的：</h3>
 * <ul>
 *   <li>将非确定性操作（HTTP 调用）从 Workflow 中分离</li>
 *   <li>支持 Temporal 的重试和错误处理机制</li>
 *   <li>保证 Workflow 重放时的一致性</li>
 *   <li>实现幂等性保护，防止重复创建文档</li>
 * </ul>
 *
 * <h3>幂等性保护：</h3>
 * <p>
 * 所有 publishCloudDoc 方法都包含 executionId、nodeId 和 retryCount 参数，
 * 用于实现幂等性检查。
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuPublishCloudDocActivityImpl
 * @see io.temporal.activity.ActivityInterface
 */
@ActivityInterface
public interface FeishuPublishCloudDocActivity {

    /**
     * 发布飞书云文档
     * <p>
     * 在飞书云空间中创建一个新的云文档。
     * 实现了幂等性保护。
     *
     * @param executionId 工作流执行 ID（用于幂等键）
     * @param retryCount 当前重试次数（用于幂等键）
     * @param nodeId 节点 ID（用于幂等键）
     * @param connectionId 飞书连接配置 ID
     * @param title 文档标题
     * @param content 文档内容（可选）
     * @return 创建成功的文档 URL
     * @throws IllegalArgumentException 如果 connectionId 为 null
     * @throws IllegalStateException 如果 API 调用失败
     */
    String publishCloudDoc(Long executionId, Integer retryCount, String nodeId,
                           Long connectionId, String title, String content);

    /**
     * 发布飞书云文档（使用连接配置）
     * <p>
     * 重载方法，允许直接传入连接配置而非 connectionId。
     * 实现了幂等性保护。
     *
     * @param executionId 工作流执行 ID（用于幂等键）
     * @param retryCount 当前重试次数（用于幂等键）
     * @param nodeId 节点 ID（用于幂等键）
     * @param connectionConfig 连接配置对象
     * @param title 文档标题
     * @param content 文档内容（可选）
     * @return 创建成功的文档 URL
     */
    String publishCloudDocWithConfig(Long executionId, Integer retryCount, String nodeId,
                                     Object connectionConfig, String title, String content);
}