package com.sipc115.helix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.entity.ExecutionPlanEntity;
import com.sipc115.helix.domain.entity.WorkflowDslEntity;
import com.sipc115.helix.domain.workflow.DslEdgeSpec;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.integration.workflow.service.WorkflowExecutionApplicationService;
import com.sipc115.helix.service.WorkflowApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流完整生命周期测试
 * <p>
 * 测试流程：创建 → 编辑保存 → 编译 → 执行
 */
@SpringBootTest(properties = {
        "rocketmq.name-server=192.168.115.23:9876",
        "rocketmq.producer.group=test-producer"
})
@ActiveProfiles("test")
class WorkflowApplicationServiceTest {

    @Autowired
    private WorkflowApplicationService workflowService;

    @Autowired(required = false)
    private WorkflowExecutionApplicationService executionService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USER = "test-user";

    /**
     * 步骤1：新建工作流（初始化的，只有 start 和 end）
     */
// ... existing code ...

    /**
     * 步骤1：新建工作流（初始化的，只有 start 和 end）
     */
    @Test
    @DisplayName("【步骤1】新建初始化工作流")
    void step1_createInitialWorkflow() throws Exception {
        System.out.println("\n========== 步骤1：新建初始化工作流 ==========");

        // 创建工作流框架（自动生成 workflowId、version 和基础节点 START/END）
        WorkflowDslEntity entity = workflowService.createWorkflow("测试工作流", TEST_USER);

        assertNotNull(entity);
        assertNotNull(entity.getId());
        assertNotNull(entity.getWorkflowId());
        assertEquals("v1.0.0", entity.getVersion());
        assertEquals("DRAFT", entity.getStatus());

        // 从实体中解析DSL
        WorkflowDsl dsl = parseDsl(entity);
        assertNotNull(dsl);
        assertEquals("测试工作流", dsl.getName());
        assertTrue(dsl.getNodes().size() >= 2, "应该至少有 START 和 END 节点");

        // 输出完整 DSL JSON
        String dslJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dsl);
        System.out.println("✓ 初始化工作流创建成功");
        System.out.println("  - ID: " + entity.getId());
        System.out.println("  - workflowId: " + entity.getWorkflowId());
        System.out.println("  - version: " + entity.getVersion());
        System.out.println("\n========== 完整 DSL JSON ==========");
        System.out.println(dslJson);
        System.out.println("==================================\n");
    }

// ... existing code ...


// ... existing code ...

    /**
     * 步骤2：编辑DSL后保存新内容
     */
    @Test
    @DisplayName("【步骤2】编辑并保存DSL")
    void step2_editAndSaveDsl() throws Exception {
        System.out.println("\n========== 步骤2：编辑并保存DSL ==========");

        // 先创建初始化工作流
        WorkflowDslEntity created = workflowService.createWorkflow("简易飞书通知工作流", TEST_USER);
        String workflowId = created.getWorkflowId();
        String version = created.getVersion();

        // 使用现成的完整 DSL
        String dslJson = """
                {
                  "workflowId": "%s",
                  "name": "简易飞书通知工作流",
                  "version": "%s",
                  "nodes": [
                    {
                      "id": "start",
                      "type": "START",
                      "name": "开始",
                      "category": "CONTROL_FLOW",
                      "policy": {
                        "retryPolicy": {
                          "maxAttempts": 3,
                          "initialInterval": 1.0,
                          "maxInterval": 10.0,
                          "backoffCoefficient": 2.0,
                          "retryableExceptions": [],
                          "nonRetryableExceptions": []
                        },
                        "timeout": {
                          "executionTimeout": 300.0,
                          "scheduleToCloseTimeout": 600.0,
                          "scheduleToStartTimeout": 30.0,
                          "startToCloseTimeout": 300.0
                        }
                      },
                      "config": {}
                    },
                    {
                      "id": "sendNotify",
                      "type": "FEISHU_SEND_TEXT",
                      "name": "发送飞书通知",
                      "category": "NOTIFICATION",
                      "policy": {
                        "retryPolicy": {
                          "maxAttempts": 3,
                          "initialInterval": 1.0,
                          "maxInterval": 10.0,
                          "backoffCoefficient": 2.0,
                          "retryableExceptions": [],
                          "nonRetryableExceptions": []
                        },
                        "timeout": {
                          "executionTimeout": 300.0,
                          "scheduleToCloseTimeout": 600.0,
                          "scheduleToStartTimeout": 30.0,
                          "startToCloseTimeout": 300.0
                        }
                      },
                      "config": {
                        "chatId": "oc_test_chat_id",
                        "text": "工作流测试消息"
                      }
                    },
                    {
                      "id": "end",
                      "type": "END",
                      "name": "结束",
                      "category": "CONTROL_FLOW",
                      "policy": {
                        "retryPolicy": {
                          "maxAttempts": 3,
                          "initialInterval": 1.0,
                          "maxInterval": 10.0,
                          "backoffCoefficient": 2.0,
                          "retryableExceptions": [],
                          "nonRetryableExceptions": []
                        },
                        "timeout": {
                          "executionTimeout": 300.0,
                          "scheduleToCloseTimeout": 600.0,
                          "scheduleToStartTimeout": 30.0,
                          "startToCloseTimeout": 300.0
                        }
                      },
                      "config": {}
                    }
                  ],
                  "edges": [
                    {
                      "from": "start",
                      "to": "sendNotify",
                      "conditionKey": null
                    },
                    {
                      "from": "sendNotify",
                      "to": "end",
                      "conditionKey": null
                    }
                  ],
                  "metadata": {
                    "scope": "PRIVATE",
                    "state": "DRAFT",
                    "description": "简易飞书通知测试工作流",
                    "owner": "test-user",
                    "tags": [],
                    "createdBy": "test-user",
                    "createdAt": null,
                    "updatedBy": "test-user",
                    "updatedAt": null,
                    "extra": {}
                  },
                  "schedule": null,
                  "state": "DRAFT",
                  "scope": "PRIVATE"
                }
                """.formatted(workflowId, version);

        // 解析 DSL
        WorkflowDsl dsl = objectMapper.readValue(dslJson, WorkflowDsl.class);

        // 保存编辑后的DSL
        WorkflowDslEntity saved = workflowService.saveWorkflow(dsl, TEST_USER);

        assertNotNull(saved);
        assertEquals(workflowId, saved.getWorkflowId());
        assertEquals(version, saved.getVersion());

        // 验证保存的内容
        WorkflowDsl savedDsl = parseDsl(saved);
        assertTrue(savedDsl.getNodes().size() > 2, "应该有 START + FEISHU + END");

        System.out.println("✓ DSL编辑并保存成功");
        System.out.println("  - workflowId: " + workflowId);
        System.out.println("  - 编辑后节点数: " + savedDsl.getNodes().size());
        System.out.println("  - 节点列表:");
        savedDsl.getNodes().forEach(node ->
                System.out.println("    * " + node.getId() + " (" + node.getType() + ")")
        );
    }

    // ... existing code ...
    /**
     * 步骤3：编译DSL生成执行计划并存储
     */
    @Test
    @DisplayName("【步骤3】编译DSL生成执行计划")
    void step3_compileAndSaveExecutionPlan() throws Exception {
        System.out.println("\n========== 步骤3：编译DSL生成执行计划 ==========");

        // 创建工作流框架
        WorkflowDslEntity created = workflowService.createWorkflow("编译测试工作流", TEST_USER);
        String workflowId = created.getWorkflowId();
        String version = created.getVersion();

        // 使用现成的完整 DSL
        String dslJson = """
            {
              "workflowId": "%s",
              "name": "编译测试工作流",
              "version": "%s",
              "nodes": [
                {
                  "id": "start",
                  "type": "START",
                  "name": "开始",
                  "category": "CONTROL_FLOW",
                  "policy": {
                    "retryPolicy": {
                      "maxAttempts": 3,
                      "initialInterval": 1.0,
                      "maxInterval": 10.0,
                      "backoffCoefficient": 2.0,
                      "retryableExceptions": [],
                      "nonRetryableExceptions": []
                    },
                    "timeout": {
                      "executionTimeout": 300.0,
                      "scheduleToCloseTimeout": 600.0,
                      "scheduleToStartTimeout": 30.0,
                      "startToCloseTimeout": 300.0
                    }
                  },
                  "config": {}
                },
                {
                  "id": "sendNotify",
                  "type": "FEISHU_SEND_TEXT",
                  "name": "发送飞书通知",
                  "category": "NOTIFICATION",
                  "policy": {
                    "retryPolicy": {
                      "maxAttempts": 3,
                      "initialInterval": 1.0,
                      "maxInterval": 10.0,
                      "backoffCoefficient": 2.0,
                      "retryableExceptions": [],
                      "nonRetryableExceptions": []
                    },
                    "timeout": {
                      "executionTimeout": 300.0,
                      "scheduleToCloseTimeout": 600.0,
                      "scheduleToStartTimeout": 30.0,
                      "startToCloseTimeout": 300.0
                    }
                  },
                  "config": {
                    "chatId": "oc_test_chat_id",
                    "text": "工作流测试消息"
                  }
                },
                {
                  "id": "end",
                  "type": "END",
                  "name": "结束",
                  "category": "CONTROL_FLOW",
                  "policy": {
                    "retryPolicy": {
                      "maxAttempts": 3,
                      "initialInterval": 1.0,
                      "maxInterval": 10.0,
                      "backoffCoefficient": 2.0,
                      "retryableExceptions": [],
                      "nonRetryableExceptions": []
                    },
                    "timeout": {
                      "executionTimeout": 300.0,
                      "scheduleToCloseTimeout": 600.0,
                      "scheduleToStartTimeout": 30.0,
                      "startToCloseTimeout": 300.0
                    }
                  },
                  "config": {}
                }
              ],
              "edges": [
                {
                  "from": "start",
                  "to": "sendNotify",
                  "conditionKey": null
                },
                {
                  "from": "sendNotify",
                  "to": "end",
                  "conditionKey": null
                }
              ],
              "metadata": {
                "scope": "PRIVATE",
                "state": "DRAFT",
                "description": "编译测试工作流",
                "owner": "test-user",
                "tags": [],
                "createdBy": "test-user",
                "createdAt": null,
                "updatedBy": "test-user",
                "updatedAt": null,
                "extra": {}
              },
              "schedule": null,
              "state": "DRAFT",
              "scope": "PRIVATE"
            }
            """.formatted(workflowId, version);

        // 解析 DSL
        WorkflowDsl dsl = objectMapper.readValue(dslJson, WorkflowDsl.class);

        // 保存 DSL
        workflowService.saveWorkflow(dsl, TEST_USER);

        // 编译并保存执行计划
        ExecutionPlanEntity planEntity = workflowService.saveAndCompile(dsl, TEST_USER);

        assertNotNull(planEntity);
        assertNotNull(planEntity.getPlanId());
        assertEquals(dsl.getWorkflowId(), planEntity.getWorkflowId());
        assertEquals(dsl.getVersion(), planEntity.getVersion());

        System.out.println("✓ 执行计划编译并存储成功");
        System.out.println("  - planId: " + planEntity.getPlanId());
        System.out.println("  - workflowId: " + planEntity.getWorkflowId());
        System.out.println("  - version: " + planEntity.getVersion());

        // 验证可以读取执行计划
        ExecutionPlan plan = workflowService.getExecutionPlan(dsl.getWorkflowId(), dsl.getVersion());
        assertNotNull(plan);
        assertEquals(dsl.getWorkflowId(), plan.getWorkflowId());
        assertNotNull(plan.getEntryNodeId());
        assertTrue(plan.getNodes().size() > 0);

        System.out.println("  - 编译后节点数: " + plan.getNodes().size());
        System.out.println("  - 入口节点: " + plan.getEntryNodeId());
        System.out.println("  - 转换数: " + plan.getTransitions().size());
    }
// ... existing code ...


    /**
     * 步骤4：执行已存储的执行计划
     */
// ... existing code ...
    /**
     * 步骤4：执行已存储的执行计划
     */
    @Test
    @DisplayName("【步骤4】执行已存储的执行计划")
    void step4_executeStoredPlan() throws Exception {
        System.out.println("\n========== 步骤4：执行已存储的执行计划 ==========");

        // 前置步骤：创建、编辑、编译
        WorkflowDslEntity created = workflowService.createWorkflow("执行测试工作流", TEST_USER);
        String workflowId = created.getWorkflowId();
        String version = created.getVersion();

        // 使用现成的完整 DSL
        String dslJson = """
            {
              "workflowId": "%s",
              "name": "执行测试工作流",
              "version": "%s",
              "nodes": [
                {
                  "id": "start",
                  "type": "START",
                  "name": "开始",
                  "category": "CONTROL_FLOW",
                  "policy": {
                    "retryPolicy": {
                      "maxAttempts": 3,
                      "initialInterval": 1.0,
                      "maxInterval": 10.0,
                      "backoffCoefficient": 2.0,
                      "retryableExceptions": [],
                      "nonRetryableExceptions": []
                    },
                    "timeout": {
                      "executionTimeout": 300.0,
                      "scheduleToCloseTimeout": 600.0,
                      "scheduleToStartTimeout": 30.0,
                      "startToCloseTimeout": 300.0
                    }
                  },
                  "config": {}
                },
                {
                  "id": "sendNotify",
                  "type": "FEISHU_SEND_TEXT",
                  "name": "发送飞书通知",
                  "category": "NOTIFICATION",
                  "policy": {
                    "retryPolicy": {
                      "maxAttempts": 3,
                      "initialInterval": 1.0,
                      "maxInterval": 10.0,
                      "backoffCoefficient": 2.0,
                      "retryableExceptions": [],
                      "nonRetryableExceptions": []
                    },
                    "timeout": {
                      "executionTimeout": 300.0,
                      "scheduleToCloseTimeout": 600.0,
                      "scheduleToStartTimeout": 30.0,
                      "startToCloseTimeout": 300.0
                    }
                  },
                  "config": {
                    "chatId": "oc_test_chat_id",
                    "text": "工作流测试消息"
                  }
                },
                {
                  "id": "end",
                  "type": "END",
                  "name": "结束",
                  "category": "CONTROL_FLOW",
                  "policy": {
                    "retryPolicy": {
                      "maxAttempts": 3,
                      "initialInterval": 1.0,
                      "maxInterval": 10.0,
                      "backoffCoefficient": 2.0,
                      "retryableExceptions": [],
                      "nonRetryableExceptions": []
                    },
                    "timeout": {
                      "executionTimeout": 300.0,
                      "scheduleToCloseTimeout": 600.0,
                      "scheduleToStartTimeout": 30.0,
                      "startToCloseTimeout": 300.0
                    }
                  },
                  "config": {}
                }
              ],
              "edges": [
                {
                  "from": "start",
                  "to": "sendNotify",
                  "conditionKey": null
                },
                {
                  "from": "sendNotify",
                  "to": "end",
                  "conditionKey": null
                }
              ],
              "metadata": {
                "scope": "PRIVATE",
                "state": "DRAFT",
                "description": "执行测试工作流",
                "owner": "test-user",
                "tags": [],
                "createdBy": "test-user",
                "createdAt": null,
                "updatedBy": "test-user",
                "updatedAt": null,
                "extra": {}
              },
              "schedule": null,
              "state": "DRAFT",
              "scope": "PRIVATE"
            }
            """.formatted(workflowId, version);

        // 解析并保存 DSL
        WorkflowDsl dsl = objectMapper.readValue(dslJson, WorkflowDsl.class);
        workflowService.saveWorkflow(dsl, TEST_USER);

        // 编译并保存执行计划
        workflowService.saveAndCompile(dsl, TEST_USER);

        // 检查执行服务是否可用
        if (executionService == null) {
            System.out.println("⚠ 跳过执行：WorkflowExecutionApplicationService 未注入");
            System.out.println("  提示：需要 Temporal Server 运行才能实际执行工作流");
            return;
        }

        try {
            // 构建执行请求
            com.sipc115.helix.domain.workflow.WorkflowExecutionRequest request =
                    new com.sipc115.helix.domain.workflow.WorkflowExecutionRequest();
            request.setWorkflowId(dsl.getWorkflowId());
            request.setWorkflowVersion(dsl.getVersion());
            request.setInput(Map.of(
                    "chatId", "oc_test_chat",
                    "text", "测试消息"
            ));

            // 启动执行
            String workflowRunId = executionService.start(request);

            assertNotNull(workflowRunId);
            System.out.println("✓ 工作流执行已启动");
            System.out.println("  - workflowRunId: " + workflowRunId);
            System.out.println("  - workflowId: " + dsl.getWorkflowId());
            System.out.println("  - version: " + dsl.getVersion());
            System.out.println("  ⚠ 工作流在 Temporal 中异步执行，请查看 Temporal UI 监控状态");

        } catch (Exception e) {
            System.err.println("✗ 执行失败: " + e.getMessage());
            System.out.println("  可能原因：Temporal Server 未运行或连接失败");
            e.printStackTrace();
        }
    }
// ... existing code ...


    /**
     * 辅助方法：解析DSL实体为领域对象
     */
    private WorkflowDsl parseDsl(WorkflowDslEntity entity) throws Exception {
        return objectMapper.readValue(entity.getDslContent(), WorkflowDsl.class);
    }

    /**
     * 辅助方法：添加飞书通知节点到DSL
     */
    private void addFeishuNode(WorkflowDsl dsl) {
        // 添加飞书节点
        DslNodeSpec feishuNode = new DslNodeSpec();
        feishuNode.setId("sendNotify");
        feishuNode.setType(DslNodeType.FEISHU_SEND_TEXT);
        feishuNode.setName("发送飞书通知");
        feishuNode.setCategory(com.sipc115.helix.domain.workflow.NodeCategory.NOTIFICATION);
        feishuNode.setConfig(Map.of(
                "chatId", "oc_test_chat_id",
                "text", "工作流测试消息"
        ));
        dsl.getNodes().add(feishuNode);

        // 修改边：START -> FEISHU -> END
        dsl.getEdges().clear();

        DslEdgeSpec edge1 = new DslEdgeSpec();
        edge1.setFrom("start");
        edge1.setTo("sendNotify");
        dsl.getEdges().add(edge1);

        DslEdgeSpec edge2 = new DslEdgeSpec();
        edge2.setFrom("sendNotify");
        edge2.setTo("end");
        dsl.getEdges().add(edge2);
    }
}
