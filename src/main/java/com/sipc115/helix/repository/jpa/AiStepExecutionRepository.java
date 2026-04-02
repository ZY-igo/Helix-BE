/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.repository.jpa;

import com.sipc115.helix.domain.workflow.AiStepExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiStepExecutionRepository extends JpaRepository<AiStepExecutionEntity, Long> {

    List<AiStepExecutionEntity> findByNodeTraceIdOrderByRound(Long nodeTraceId);
}
