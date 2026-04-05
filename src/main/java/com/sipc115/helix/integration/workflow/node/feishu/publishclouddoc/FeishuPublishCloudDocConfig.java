/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc;

import lombok.Data;

/**
 * 飞书发布云文档节点配置
 * <p>
 * 用于配置飞书云文档创建节点的参数。
 * 在飞书中创建一个新的云文档（文档类型）。
 *
 * <h3>DSL 中的配置方式：</h3>
 * <pre>
 * {
 *   "type": "FEISHU_PUBLISH_CLOUD_DOC",
 *   "config": {
 *     "connectionId": 123,
 *     "title": "AI 分析报告",
 *     "content": "${aiNode.result}"
 *   }
 * }
 * </pre>
 *
 * <h3>字段说明：</h3>
 * <ul>
 *   <li>connectionId - 飞书连接ID，指向 FEISHU 类型的集成连接</li>
 *   <li>title - 文档标题</li>
 *   <li>content - 文档内容（目前只创建空白文档，content 暂未使用）</li>
 * </ul>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>当前版本只创建空白文档，content 内容会忽略</li>
 *   <li>后续版本会支持在创建文档后追加内容块</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuPublishCloudDocNodeExecutor
 * @see FeishuPublishCloudDocNodeDefinition
 */
@Data
public class FeishuPublishCloudDocConfig {

    /**
     * 飞书连接 ID
     * <p>
     * 指向 IntegrationConnection 表中 type='FEISHU' 的记录。
     */
    private Long connectionId;

    /**
     * 文档标题
     * <p>
     * 创建的云文档的标题。
     * 支持表达式 ${variable.name}。
     */
    private String title;

    /**
     * 文档内容
     * <p>
     * 文档的正文内容。
     * 注意：当前版本暂不支持内容写入，只创建空白文档。
     */
    private String content;
}
