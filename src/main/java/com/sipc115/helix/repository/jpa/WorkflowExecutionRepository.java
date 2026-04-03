/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.repository.jpa;

import com.sipc115.helix.domain.workflow.WorkflowExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecutionEntity, Long> {

    List<WorkflowExecutionEntity> findByWorkflowId(String workflowId);

    Optional<WorkflowExecutionEntity> findByWorkflowIdAndVersion(String workflowId, String version);

    List<WorkflowExecutionEntity> findByWorkflowIdOrderByCreatedAtDesc(String workflowId);
}
