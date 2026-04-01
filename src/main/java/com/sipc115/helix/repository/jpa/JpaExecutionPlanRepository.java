/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.repository.jpa;

import com.sipc115.helix.domain.entity.ExecutionPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 执行计划 JPA 仓储接口
 */
@Repository
public interface JpaExecutionPlanRepository extends JpaRepository<ExecutionPlanEntity, Long> {

    /**
     * 根据工作流 ID 和版本查询
     */
    Optional<ExecutionPlanEntity> findByWorkflowIdAndVersion(String workflowId, Integer version);

    /**
     * 根据计划 ID 查询
     */
    Optional<ExecutionPlanEntity> findByPlanId(String planId);
}
