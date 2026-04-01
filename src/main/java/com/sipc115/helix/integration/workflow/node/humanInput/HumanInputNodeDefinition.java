/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.humanInput;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

/**
 * 人工输入节点定义
 * <p>
 * 定义人工输入节点（HUMAN_INPUT）的元数据和配置类型。
 * 人工输入节点用于暂停工作流执行，等待人工输入信号后继续。
 * <p>
 * DSL 配置结构：
 * <pre>
 * {
 *   "id": "approve-request",
 *   "type": "HUMAN_INPUT",
 *   "config": {
 *     "title": "审批请求",
 *     "description": "请审批该申请",
 *     "form": {
 *       "fields": [
 *         {
 *           "name": "approved",
 *           "type": "boolean",
 *           "label": "是否通过",
 *           "required": true
 *         },
 *         {
 *           "name": "comment",
 *           "type": "string",
 *           "label": "审批意见",
 *           "required": false
 *         }
 *       ],
 *       "submitText": "提交",
 *       "cancelText": "取消"
 *     },
 *     "timeout": 3600000,
 *     "outputVar": "approvalResult"
 *   }
 * }
 * </pre>
 * <p>
 * 配置字段说明：
 * <ul>
 *   <li>title: 表单标题</li>
 *   <li>description: 表单描述信息</li>
 *   <li>form: 表单配置
 *     <ul>
 *       <li>fields: 表单字段列表，每个字段包含 name、type、label、required</li>
 *       <li>submitText: 提交按钮文本（可选，默认"提交"）</li>
 *       <li>cancelText: 取消按钮文本（可选，默认"取消"）</li>
 *     </ul>
 *   </li>
 *   <li>timeout: 超时时间（毫秒），超过此时间未输入则工作流失败（可选）</li>
 *   <li>outputVar: 输出变量名，人工输入结果将存储在此变量中</li>
 * </ul>
 * <p>
 * 执行行为：
 * <ul>
 *   <li>工作流执行到此节点时暂停</li>
 *   <li>等待外部通过 Signal 机制发送人工输入</li>
 *   <li>收到输入后，将输入数据存储到 outputVar 指定的变量中</li>
 *   <li>工作流继续执行</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class HumanInputNodeDefinition implements NodeDefinition<HumanInputNodeConfig> {

    /**
     * 获取节点类型
     *
     * @return HUMAN_INPUT 节点类型枚举
     */
    @Override
    public DslNodeType type() {
        return DslNodeType.HUMAN_INPUT;
    }

    /**
     * 获取配置类
     *
     * @return HumanInputNodeConfig 配置类
     */
    @Override
    public Class<HumanInputNodeConfig> configClass() {
        return HumanInputNodeConfig.class;
    }
}
