package com.sipc115.helix.controller;

import com.sipc115.helix.domain.entity.ExecutionPlanEntity;
import com.sipc115.helix.domain.entity.WorkflowDslEntity;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.service.WorkflowApplicationService;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowApplicationService workflowService;

    public WorkflowController(WorkflowApplicationService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/create")
    public ResponseEntity<WorkflowDslEntity> createWorkflow(@RequestBody CreateWorkflowRequest request) {
        log.info("REST: Create workflow. name={}", request.getName());

        String createdBy = request.getCreatedBy() != null ? request.getCreatedBy() : "system";
        WorkflowDslEntity created = workflowService.createWorkflow(request.getName(), createdBy);

        return ResponseEntity.ok(created);
    }

    @PostMapping("/save")
    public ResponseEntity<WorkflowDslEntity> saveWorkflow(@RequestBody SaveWorkflowRequest request) {
        log.info("REST: Save workflow. workflowId={}, version={}",
                request.getDsl().getWorkflowId(), request.getDsl().getVersion());

        String updatedBy = request.getUpdatedBy() != null ? request.getUpdatedBy() : "system";
        WorkflowDslEntity saved = workflowService.saveWorkflow(request.getDsl(), updatedBy);

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/copy")
    public ResponseEntity<WorkflowDslEntity> copyWorkflow(@RequestBody CopyWorkflowRequest request) {
        log.info("REST: Copy workflow. source={} v{}, newVersion={}",
                request.getSourceWorkflowId(), request.getSourceVersion(), request.getNewVersion());

        String createdBy = request.getCreatedBy() != null ? request.getCreatedBy() : "system";
        WorkflowDslEntity copied;

        if (request.getNewWorkflowId() != null) {
            copied = workflowService.createCopyAsNewWorkflow(
                    request.getSourceWorkflowId(),
                    request.getSourceVersion(),
                    request.getNewWorkflowId(),
                    request.getNewVersion(),
                    createdBy
            );
        } else {
            copied = workflowService.createCopy(
                    request.getSourceWorkflowId(),
                    request.getSourceVersion(),
                    request.getNewVersion(),
                    createdBy
            );
        }

        return ResponseEntity.ok(copied);
    }

    @GetMapping
    public ResponseEntity<WorkflowDslEntity> getWorkflow(
            @RequestParam String workflowId,
            @RequestParam String version) {
        log.debug("REST: Get workflow. workflowId={}, version={}", workflowId, version);

        WorkflowDslEntity workflow = workflowService.getWorkflow(workflowId, version);
        return ResponseEntity.ok(workflow);
    }

    @GetMapping("/latest-published")
    public ResponseEntity<WorkflowDslEntity> getLatestPublishedWorkflow(@RequestParam String workflowId) {
        log.debug("REST: Get latest published workflow. workflowId={}", workflowId);

        WorkflowDslEntity workflow = workflowService.getLatestPublishedWorkflow(workflowId);
        return ResponseEntity.ok(workflow);
    }

    @GetMapping("/versions")
    public ResponseEntity<List<WorkflowDslEntity>> getAllVersions(@RequestParam String workflowId) {
        log.debug("REST: Get all versions. workflowId={}", workflowId);

        List<WorkflowDslEntity> versions = workflowService.getAllVersions(workflowId);
        return ResponseEntity.ok(versions);
    }

    @PostMapping("/publish")
    public ResponseEntity<WorkflowDslEntity> publishWorkflow(
            @RequestParam String workflowId,
            @RequestParam String version,
            @RequestParam(required = false) String updatedBy) {
        log.info("REST: Publish workflow. workflowId={}, version={}", workflowId, version);

        String user = updatedBy != null ? updatedBy : "system";
        WorkflowDslEntity published = workflowService.publish(workflowId, version, user);

        return ResponseEntity.ok(published);
    }

    @PostMapping("/save-and-compile")
    public ResponseEntity<Map<String, Object>> saveAndCompile(@RequestBody SaveAndCompileRequest request) {
        log.info("REST: Save and compile workflow. workflowId={}, version={}",
                request.getDsl().getWorkflowId(), request.getDsl().getVersion());

        String compiledBy = request.getCompiledBy() != null ? request.getCompiledBy() : "system";

        WorkflowDslEntity dslSaved = workflowService.saveWorkflow(request.getDsl(), compiledBy);

        ExecutionPlanEntity planSaved = workflowService.saveAndCompile(request.getDsl(), compiledBy);

        Map<String, Object> result = new HashMap<>();
        result.put("dsl", dslSaved);
        result.put("executionPlan", planSaved);
        result.put("success", true);

        return ResponseEntity.ok(result);
    }

    @Data
    public static class CreateWorkflowRequest {
        private String name;
        private String createdBy;
    }

    @Data
    public static class SaveWorkflowRequest {
        private WorkflowDsl dsl;
        private String updatedBy;
    }

    @Data
    public static class CopyWorkflowRequest {
        private String sourceWorkflowId;
        private String sourceVersion;
        private String newWorkflowId;
        private String newVersion;
        private String createdBy;
    }

    @Data
    public static class SaveAndCompileRequest {
        private WorkflowDsl dsl;
        private String compiledBy;
    }
}
