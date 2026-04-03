/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.repository;

import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.domain.workflow.WorkflowMetadata;
import com.sipc115.helix.integration.workflow.spi.DslRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryDslRepository implements DslRepository {
    private final ConcurrentHashMap<String, WorkflowDsl> store = new ConcurrentHashMap<>();

    @Override
    public void save(WorkflowDsl dsl) {
        store.put(key(dsl.getWorkflowId(), dsl.getVersion()), dsl);
    }

    @Override
    public Optional<WorkflowDsl> findByWorkflowIdAndVersion(String workflowId, String version) {
        return Optional.ofNullable(store.get(key(workflowId, version)));
    }

    @Override
    public List<WorkflowDsl> findAllVersions(String workflowId) {
        return store.values().stream()
                .filter(dsl -> dsl.getWorkflowId().equals(workflowId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<WorkflowDsl> findLatestPublished(String workflowId) {
        return store.values().stream()
                .filter(dsl -> dsl.getWorkflowId().equals(workflowId))
                .filter(dsl -> dsl.getMetadata() != null
                        && WorkflowMetadata.WorkflowState.PUBLISHED.equals(dsl.getMetadata().getState()))
                .findFirst();
    }

    private String key(String workflowId, String version) {
        return workflowId + ":" + version;
    }
}
