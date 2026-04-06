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
     *
     * @param connectionId 飞书连接配置 ID
     * @param title 文档标题
     * @param content 文档内容（可选）
     * @return 创建成功的文档 URL
     * @throws IllegalArgumentException 如果 connectionId 为 null
     * @throws IllegalStateException 如果 API 调用失败
     */
    String publishCloudDoc(Long connectionId, String title, String content);

    /**
     * 发布飞书云文档（使用连接配置）
     *
     * @param connectionConfig 连接配置对象
     * @param title 文档标题
     * @param content 文档内容（可选）
     * @return 创建成功的文档 URL
     */
    String publishCloudDocWithConfig(Object connectionConfig, String title, String content);
}