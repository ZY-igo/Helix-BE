/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 工作流运行时桥接接口
 * <p>
 * 定义工作流运行时与底层执行引擎（如 Temporal）之间的桥接方法，
 * 提供活动任务执行、子工作流调用、持久化睡眠、人工信号等待和飞书通知等功能。
 * <p>
 * 此接口隔离了工作流执行逻辑与具体的执行引擎实现，便于替换不同的执行引擎。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public interface WorkflowRuntimeBridge {

    /**
     * 等待人工信号
     * <p>
     * 暂停工作流执行，直到接收到指定节点的人工输入信号。
     * 
     * @param expectedNodeId 期望接收信号的节点 ID
     * @return 人工输入的有效载荷，包含节点 ID 和输入数据
     */
    HumanSignalPayload awaitHumanSignal(String expectedNodeId);

    /**
     * 发送飞书文本消息
     * <p>
     * 向指定的飞书聊天发送文本消息。
     * 
     * @param chatId 聊天 ID
     * @param text 消息内容
     * @return 发送结果
     */
    boolean sendFeishuText(String chatId, String text);

    /**
     * 发送飞书富文本消息
     * <p>
     * 向指定的飞书聊天发送富文本消息。
     * 
     * @param chatId 聊天 ID
     * @param title 消息标题
     * @param lines 消息内容行列表
     * @return 发送结果
     */
    boolean sendFeishuPost(String chatId, String title, List<String> lines);

    /**
     * 发送带链接的飞书富文本消息
     * <p>
     * 向指定的飞书聊天发送带有链接的富文本消息。
     * 
     * @param chatId 聊天 ID
     * @param title 消息标题
     * @param text 消息内容
     * @param url 链接 URL
     * @param linkText 链接文本
     * @return 发送结果
     */
    boolean sendFeishuPostWithLink(String chatId, String title, String text, String url, String linkText);

    /**
     * 发布飞书云文档
     * <p>
     * 发布飞书云文档并返回文档 URL。
     * 
     * @param title 文档标题
     * @param content 文档内容
     * @return 文档 URL
     */
    String publishFeishuCloudDoc(String title, String content);
}
