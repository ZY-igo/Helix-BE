/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

/**
 * 飞书发布云文档节点定义
 * <p>
 * 定义飞书发布云文档节点（FEISHU_PUBLISH_CLOUD_DOC）的元数据。
 *
 * <h3>DSL 配置示例：</h3>
 * <pre>
 * {
 *   "id": "publishCloudDoc",
 *   "type": "FEISHU_PUBLISH_CLOUD_DOC",
 *   "name": "发布飞书云文档",
 *   "category": "NOTIFICATION",
 *   "config": {
 *     "connectionId": 123,
 *     "title": "工作流执行报告",
 *     "content": "# 工作流报告\n\n## 执行结果\n\n- 任务 1: 完成\n- 任务 2: 完成"
 *   }
 * }
 * </pre>
 *
 * <h3>配置参数：</h3>
 * <ul>
 *   <li>connectionId - 飞书连接 ID（必填，指向 FEISHU 类型的集成连接）</li>
 *   <li>title - 文档标题（必填）</li>
 *   <li>content - 文档内容（必填，支持 Markdown 格式）</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuPublishCloudDocNodeDefinition implements NodeDefinition<FeishuPublishCloudDocConfig> {

    @Override
    public DslNodeType type() {
        return DslNodeType.FEISHU_PUBLISH_CLOUD_DOC;
    }

    @Override
    public Class<FeishuPublishCloudDocConfig> configClass() {
        return FeishuPublishCloudDocConfig.class;
    }
}
