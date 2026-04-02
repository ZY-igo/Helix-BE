/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.repository.jpa;

import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeExecutionTraceRepository extends JpaRepository<NodeExecutionTraceEntity, Long> {

    List<NodeExecutionTraceEntity> findByExecutionIdOrderByExecutionOrder(Long executionId);

    List<NodeExecutionTraceEntity> findByExecutionIdAndNodeId(Long executionId, String nodeId);
}
