/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.repository.jpa;

import com.sipc115.helix.domain.entity.WorkflowDslEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 工作流 DSL JPA 仓储接口
 */
@Repository
public interface JpaWorkflowDslRepository extends JpaRepository<WorkflowDslEntity, Long> {

    /**
     * 根据工作流 ID 和版本查询
     */
    Optional<WorkflowDslEntity> findByWorkflowIdAndVersion(String workflowId, Integer version);

    /**
     * 检查工作流 ID 和版本是否存在
     */
    boolean existsByWorkflowIdAndVersion(String workflowId, Integer version);
}
