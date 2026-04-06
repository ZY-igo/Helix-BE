package com.sipc115.helix.controller;

import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.integration.connect.IntegrationConnectionService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 集成连接管理控制器
 * <p>
 * 提供外部服务集成连接（如飞书、LLM、数据库等）的增删改查及测试接口。
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>POST /api/connections - 创建连接</li>
 *   <li>PUT /api/connections/{id} - 更新连接配置</li>
 *   <li>DELETE /api/connections/{id} - 删除连接</li>
 *   <li>GET /api/connections - 获取所有连接列表</li>
 *   <li>GET /api/connections/{id} - 获取单个连接详情</li>
 *   <li>GET /api/connections/by-type/{type} - 按类型获取连接</li>
 *   <li>POST /api/connections/{id}/test - 测试连接可用性</li>
 *   <li>GET /api/connections/supported-types - 获取支持的连接类型</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see IntegrationConnectionService
 */
@RestController
@RequestMapping("/api/connections")
public class IntegrationConnectionController {

    private static final Logger log = LoggerFactory.getLogger(IntegrationConnectionController.class);

    private final IntegrationConnectionService connectionService;

    public IntegrationConnectionController(IntegrationConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * 创建集成连接
     *
     * @param request 包含连接名称、类型和配置的请求体
     * @return 创建的连接实体
     */
    @PostMapping
    public ResponseEntity<IntegrationConnection> createConnection(@RequestBody CreateConnectionRequest request) {
        log.info("REST: Create connection. name={}, type={}", request.getName(), request.getType());
        IntegrationConnection connection = connectionService.createConnection(
                request.getName(),
                request.getType(),
                request.getConfig()
        );
        return ResponseEntity.ok(connection);
    }

    /**
     * 更新集成连接配置
     *
     * @param id      连接 ID
     * @param request 包含连接名称和配置的请求体
     * @return 更新后的连接实体
     */
    @PutMapping("/{id}")
    public ResponseEntity<IntegrationConnection> updateConnection(
            @PathVariable Long id,
            @RequestBody UpdateConnectionRequest request) {
        log.info("REST: Update connection. id={}", id);
        IntegrationConnection connection = connectionService.updateConnection(
                id,
                request.getName(),
                request.getConfig()
        );
        return ResponseEntity.ok(connection);
    }

    /**
     * 删除集成连接
     *
     * @param id 连接 ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
        log.info("REST: Delete connection. id={}", id);
        connectionService.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取所有集成连接列表
     *
     * @return 连接实体列表
     */
    @GetMapping
    public ResponseEntity<List<IntegrationConnection>> getAllConnections() {
        log.debug("REST: Get all connections");
        return ResponseEntity.ok(connectionService.getAllConnections());
    }

    /**
     * 获取单个集成连接详情
     *
     * @param id 连接 ID
     * @return 连接实体
     */
    @GetMapping("/{id}")
    public ResponseEntity<IntegrationConnection> getConnection(@PathVariable Long id) {
        log.debug("REST: Get connection by id: {}", id);
        return ResponseEntity.ok(connectionService.getConnection(id));
    }

    /**
     * 按类型获取连接列表
     *
     * @param type 连接类型（如 FEISHU、LLM）
     * @return 连接实体列表
     */
    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<IntegrationConnection>> getConnectionsByType(@PathVariable String type) {
        log.debug("REST: Get connections by type: {}", type);
        return ResponseEntity.ok(connectionService.getConnectionsByType(type));
    }

    /**
     * 测试连接可用性
     *
     * @param id 连接 ID
     * @return 测试结果，包含 success 和 message 字段
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        log.info("REST: Test connection. id={}", id);
        Map<String, Object> result = new HashMap<>();
        try {
            connectionService.testConnection(id);
            result.put("success", true);
            result.put("message", "连接测试成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("连接测试失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "连接测试失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 获取系统支持的所有连接类型
     *
     * @return 连接类型字符串列表
     */
    @GetMapping("/supported-types")
    public ResponseEntity<List<String>> getSupportedTypes() {
        return ResponseEntity.ok(connectionService.getSupportedTypes());
    }

    @Data
    public static class CreateConnectionRequest {
        private String name;
        private String type;
        private Map<String, Object> config;
    }

    @Data
    public static class UpdateConnectionRequest {
        private String name;
        private Map<String, Object> config;
    }
}
