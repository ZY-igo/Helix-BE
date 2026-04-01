/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.feishu;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import org.springframework.stereotype.Component;

/**
 * 飞书通知节点编译器
 * <p>
 * 负责编译飞书通知节点（FEISHU_NOTIFICATION），将 DSL 节点规范转换为编译后的节点。
 * <p>
 * 验证规则：
 * <ul>
 *   <li>必须指定 action 类型</li>
 *   <li>根据 action 类型验证必要参数：</li>
 *   <ul>
 *     <li>sendText: 需要 text 参数</li>
 *     <li>sendPost: 需要 title 和 lines 参数</li>
 *     <li>sendPostWithLink: 需要 title、text 和 url 参数</li>
 *     <li>publishCloudDoc: 需要 title 和 content 参数</li>
 *   </ul>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuNotificationNodeCompiler implements NodeCompiler {

    /**
     * 获取支持的节点类型
     *
     * @return FEISHU_NOTIFICATION 节点类型
     */
    @Override
    public DslNodeType supportType() {
        return DslNodeType.FEISHU_NOTIFICATION;
    }

    /**
     * 验证节点配置
     * <p>
     * 验证飞书通知节点的配置是否合法：
     * <ol>
     *   <li>验证节点规范不为 null</li>
     *   <li>验证 action 类型已指定</li>
     *   <li>根据 action 类型验证必要参数</li>
     * </ol>
     *
     * @param source DSL 节点规范
     * @param context 编译上下文
     * @throws IllegalArgumentException 当配置不合法时抛出
     */
    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        // 基本验证：节点规范不能为 null
        if (source == null) {
            throw new IllegalArgumentException("Node spec cannot be null");
        }

        // 验证动作类型已指定
        String action = (String) source.getConfig().get("action");
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }

        // 根据动作类型验证必要参数
        switch (action) {
            case "sendText":
                // 发送文本消息需要 text 参数
                if (source.getConfig().get("text") == null) {
                    throw new IllegalArgumentException("Text cannot be null for sendText action");
                }
                break;
            case "sendPost":
                // 发送富文本消息需要 title 和 lines 参数
                if (source.getConfig().get("title") == null || source.getConfig().get("lines") == null) {
                    throw new IllegalArgumentException("Title and lines cannot be null for sendPost action");
                }
                break;
            case "sendPostWithLink":
                // 发送带链接的富文本消息需要 title、text 和 url 参数
                if (source.getConfig().get("title") == null || source.getConfig().get("text") == null ||
                    source.getConfig().get("url") == null) {
                    throw new IllegalArgumentException("Title, text and url cannot be null for sendPostWithLink action");
                }
                break;
            case "publishCloudDoc":
                // 发布云文档需要 title 和 content 参数
                if (source.getConfig().get("title") == null || source.getConfig().get("content") == null) {
                    throw new IllegalArgumentException("Title and content cannot be null for publishCloudDoc action");
                }
                break;
            default:
                // 未知的 action 类型
                throw new IllegalArgumentException("Unknown action type: " + action);
        }
    }

    /**
     * 编译节点
     * <p>
     * 将 DSL 节点规范编译为 CompiledNode：
     * <ol>
     *   <li>创建 CompiledNode 实例</li>
     *   <li>设置节点 ID 和类型</li>
     *   <li>复制节点配置</li>
     * </ol>
     *
     * @param source DSL 节点规范
     * @param context 编译上下文
     * @return 编译后的节点
     */
    @Override
    public CompiledNode compile(DslNodeSpec source, CompileContext context) {
        // 创建编译后的节点
        CompiledNode node = new CompiledNode();
        node.setId(source.getId());
        node.setType(source.getType());

        // 复制配置
        if (source.getConfig() != null) {
            node.setConfig(source.getConfig());
        }

        return node;
    }
}
