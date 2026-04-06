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
 *
 * <p>提供外部服务集成连接（如飞书、LLM、数据库等）的增删改查及测试接口。</p>
 *
 * @author Helix Team
 * @since 2.0.0
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
     * 创建新的集成连接
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
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
        log.info("REST: Delete connection. id={}", id);
        connectionService.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取所有集成连接列表
     */
    @GetMapping
    public ResponseEntity<List<IntegrationConnection>> getAllConnections() {
        log.debug("REST: Get all connections");
        return ResponseEntity.ok(connectionService.getAllConnections());
    }

    /**
     * 获取单个集成连接详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<IntegrationConnection> getConnection(@PathVariable Long id) {
        log.debug("REST: Get connection by id: {}", id);
        return ResponseEntity.ok(connectionService.getConnection(id));
    }

    /**
     * 根据类型获取连接列表 (例如: /api/connections/by-type/FEISHU)
     */
    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<IntegrationConnection>> getConnectionsByType(@PathVariable String type) {
        log.debug("REST: Get connections by type: {}", type);
        return ResponseEntity.ok(connectionService.getConnectionsByType(type));
    }

    /**
     * 测试连接可用性
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
