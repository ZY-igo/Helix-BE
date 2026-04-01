/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.spi;

import com.sipc115.helix.domain.workflow.WorkflowDsl;

import java.util.Optional;

/**
 * DSL 存储库接口
 * <p>
 * 定义工作流 DSL 的存储和查询操作，是 DSL 持久化的抽象接口。
 * 实现类负责具体的存储逻辑，如内存存储、数据库存储等。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public interface DslRepository {
    /**
     * 保存工作流 DSL
     * <p>
     * 将工作流 DSL 存储到存储库中，确保 DSL 可以被后续查询和使用。
     * 
     * @param dsl 工作流 DSL 对象
     */
    void save(WorkflowDsl dsl);
    
    /**
     * 根据工作流 ID 和版本查询 DSL
     * <p>
     * 从存储库中查询指定工作流 ID 和版本的 DSL 对象。
     * 
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 包含工作流 DSL 的 Optional 对象，如果不存在则返回 Optional.empty()
     */
    Optional<WorkflowDsl> findByWorkflowIdAndVersion(String workflowId, Integer version);
}
