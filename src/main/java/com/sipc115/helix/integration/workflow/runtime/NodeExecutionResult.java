package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.ExecutionStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点执行结果类
 * <p>
 * 用于表示工作流节点的执行结果，包含执行状态、下一个节点ID、分支键和输出等信息
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Data
public class NodeExecutionResult {
    /**
     * 执行状态
     * -- GETTER --
     *  获取执行状态
     *
     *
     * -- SETTER --
     *  设置执行状态
     *
     @return 执行状态
      * @param status 执行状态

     */
    private ExecutionStatus status;
    
    /**
     * 下一个节点ID
     * -- GETTER --
     *  获取下一个节点ID
     *
     *
     * -- SETTER --
     *  设置下一个节点ID
     *
     @return 下一个节点ID
      * @param nextNodeId 下一个节点ID

     */
    private String nextNodeId;
    
    /**
     * 分支键
     * -- GETTER --
     *  获取分支键
     *
     *
     * -- SETTER --
     *  设置分支键
     *
     @return 分支键
      * @param branchKey 分支键

     */
    private String branchKey;
    
    /**
     * 输出结果
     * -- GETTER --
     *  获取输出结果
     *
     *
     * -- SETTER --
     *  设置输出结果
     *
     @return 输出结果
      * @param output 输出结果

     */
    private Map<String, Object> output = new HashMap<>();

    /**
     * 创建一个已完成状态的执行结果
     * 
     * @return 已完成状态的执行结果
     */
    public static NodeExecutionResult completed() {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setStatus(ExecutionStatus.COMPLETED);
        return result;
    }

    /**
     * 创建一个等待信号状态的执行结果
     * 
     * @return 等待信号状态的执行结果
     */
    public static NodeExecutionResult waiting() {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setStatus(ExecutionStatus.WAITING_SIGNAL);
        return result;
    }

}
