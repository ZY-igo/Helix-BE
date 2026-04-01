/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.feishu;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.engine.ActivityFactory;
import com.sipc115.helix.integration.workflow.engine.ActivityInvocationSpec;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 飞书通知节点执行器
 * <p>
 * 实现飞书通知节点的执行逻辑，支持多种通知类型：
 * <ul>
 *   <li>sendText: 发送文本消息</li>
 *   <li>sendPost: 发送富文本消息</li>
 *   <li>sendPostWithLink: 发送带链接的富文本消息</li>
 *   <li>publishCloudDoc: 发布云文档</li>
 * </ul>
 * <p>
 * 使用 Activity Factory 模式从 WorkflowRuntimeBridge 获取飞书通知 Activity 实例，
 * 支持节点级的 Activity 配置（超时、队列、重试策略等）。
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuNotificationNodeExecutor implements WorkflowNodeExecutor {

    /**
     * 判断是否支持指定类型的节点
     *
     * @param type 节点类型字符串
     * @return 如果支持 FEISHU_NOTIFICATION 类型则返回 true
     */
    @Override
    public boolean supports(String type) {
        return DslNodeType.FEISHU_NOTIFICATION.name().equals(type);
    }

    /**
     * 执行飞书通知节点
     * <p>
     * 根据节点配置执行相应的飞书通知操作：
     * <ol>
     *   <li>从节点配置中解析 action 类型</li>
     *   <li>从 Bridge 获取 Activity Factory</li>
     *   <li>根据节点配置构建 ActivityInvocationSpec</li>
     *   <li>创建 FeishuNotificationActivity 实例</li>
     *   <li>执行相应的通知操作</li>
     *   <li>返回执行结果</li>
     * </ol>
     * <p>
     * 异常处理策略：
     * <ul>
     *   <li>系统异常（网络错误、服务不可用等）直接抛出，由 Temporal 处理重试</li>
     *   <li>业务失败（如消息发送失败）返回 failure 分支</li>
     * </ul>
     *
     * @param node 编译后的节点定义
     * @param context 执行上下文
     * @param bridge 工作流运行时桥接
     * @return 节点执行结果，包含分支键（success/failure）和输出数据
     * @throws RuntimeException 当发生系统异常时抛出
     */
    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        Map<String, Object> config = node.getConfig();
        String action = (String) config.getOrDefault("action", "sendText");
        String chatId = getStringValue(config, "chatId");

        // 从工厂获取 Activity（带节点级配置）
        // 使用 ActivityInvocationSpec.fromNodeConfig 从节点配置解析 Activity 配置
        ActivityInvocationSpec spec = ActivityInvocationSpec.fromNodeConfig(config);
        ActivityFactory factory = bridge.activities();
        FeishuNotificationActivity feishu = factory.getActivity(FeishuNotificationActivity.class, spec);

        boolean success = false;
        String result = null;

        try {
            switch (action) {
                case "sendText":
                    // 发送文本消息
                    String text = getStringValue(config, "text");
                    success = feishu.sendText(chatId, text);
                    break;

                case "sendPost":
                    // 发送富文本消息
                    String title = getStringValue(config, "title");
                    List<String> lines = getStringListValue(config, "lines");
                    success = feishu.sendPost(chatId, title, lines);
                    break;

                case "sendPostWithLink":
                    // 发送带链接的富文本消息
                    String postTitle = getStringValue(config, "title");
                    String postText = getStringValue(config, "text");
                    String url = getStringValue(config, "url");
                    String linkText = getStringValue(config, "linkText");
                    success = feishu.sendPostWithLink(chatId, postTitle, postText, url, linkText);
                    break;

                case "publishCloudDoc":
                    // 发布云文档
                    String docTitle = getStringValue(config, "title");
                    String content = getStringValue(config, "content");
                    result = feishu.publishCloudDoc(docTitle, content);
                    success = result != null;
                    break;

                default:
                    throw new IllegalArgumentException("Unknown action type: " + action);
            }
        } catch (Exception e) {
            // 系统异常直接抛出，让 Temporal 处理重试/失败
            // 这样可以利用 Temporal 的重试机制和故障恢复能力
            throw new RuntimeException("Feishu notification failed: " + e.getMessage(), e);
        }

        // 构建执行结果
        NodeExecutionResult executionResult = NodeExecutionResult.completed();
        executionResult.setBranchKey(success ? "success" : "failure");

        // 如果有返回值（如云文档 URL），放入输出
        if (result != null) {
            executionResult.getOutput().put("result", result);
        }

        return executionResult;
    }

    /**
     * 从配置中获取字符串值
     *
     * @param config 节点配置映射
     * @param key 配置键
     * @return 字符串值，如果不存在则返回 null
     */
    private String getStringValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 从配置中获取字符串列表值
     *
     * @param config 节点配置映射
     * @param key 配置键
     * @return 字符串列表，如果不存在或类型不匹配则返回 null
     */
    @SuppressWarnings("unchecked")
    private List<String> getStringListValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value instanceof List ? (List<String>) value : null;
    }
}
