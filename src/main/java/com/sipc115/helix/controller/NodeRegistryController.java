/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.controller;

import com.sipc115.helix.service.NodeRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 节点注册表控制器
 * <p>
 * 提供工作流节点注册表的查询 RESTful API 接口。
 * 节点注册表管理着所有已注册的工作流节点类型（如开始节点、结束节点、AI任务节点、条件节点等），
 * 通过此控制器可以查询节点类型列表、节点定义信息、配置示例等。
 * </p>
 *
 * <p>主要功能包括：</p>
 * <ul>
 *   <li>获取所有已注册的节点类型列表</li>
 *   <li>获取所有节点定义详细信息</li>
 *   <li>根据类型查询特定节点的定义信息</li>
 *   <li>获取节点的配置类名和示例配置</li>
 *   <li>检查节点类型是否已注册</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/node-registry")
public class NodeRegistryController {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(NodeRegistryController.class);

    /**
     * 节点注册表服务
     */
    @Autowired
    private NodeRegistryService nodeRegistryService;

    /**
     * 获取所有已注册的节点类型列表
     * <p>
     * 返回系统中所有已注册的节点类型名称。
     * 可用于前端构建节点选择器或验证节点类型有效性。
     * </p>
     *
     * @return 节点类型名称列表
     */
    @GetMapping("/types")
    public ResponseEntity<List<String>> getAllRegisteredTypes() {
        log.debug("Fetching all registered node types");

        List<String> types = nodeRegistryService.getAllRegisteredTypes();
        log.debug("Found {} registered node types", types.size());

        return ResponseEntity.ok(types);
    }

    /**
     * 获取所有已注册的节点定义信息
     * <p>
     * 返回系统中所有节点的详细信息，包括类型、角色、配置类等。
     * 可用于动态表单生成、节点配置UI等场景。
     * </p>
     *
     * @return 节点定义信息列表
     */
    @GetMapping("/definitions")
    public ResponseEntity<List<NodeRegistryService.NodeDefinitionInfo>> getAllNodeDefinitions() {
        log.debug("Fetching all node definitions");

        List<NodeRegistryService.NodeDefinitionInfo> definitions = nodeRegistryService.getAllNodeDefinitions();
        log.debug("Found {} node definitions", definitions.size());

        return ResponseEntity.ok(definitions);
    }

    /**
     * 获取特定节点的定义信息
     * <p>
     * 根据节点类型获取该节点的详细定义信息。
     * 包含节点类型、类型值、角色、配置类名和定义类名。
     * </p>
     *
     * @param type 节点类型名称（如 "AI_TASK", "START", "END" 等）
     * @return 节点定义信息，如果类型不存在返回 404
     */
    @GetMapping("/definitions/{type}")
    public ResponseEntity<NodeRegistryService.NodeDefinitionInfo> getNodeDefinition(@PathVariable String type) {
        log.debug("Fetching node definition for type={}", type);

        Optional<NodeRegistryService.NodeDefinitionInfo> definition = nodeRegistryService.getNodeDefinition(type);
        return definition.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 获取节点的配置类名称
     * <p>
     * 返回指定节点类型对应的配置类的全限定名。
     * 可用于动态类加载和配置实例化。
     * </p>
     *
     * @param type 节点类型名称
     * @return 配置类全限定名，如果类型不存在返回 404
     */
    @GetMapping("/config-class/{type}")
    public ResponseEntity<String> getConfigClassName(@PathVariable String type) {
        log.debug("Fetching config class name for type={}", type);

        Optional<String> configClassName = nodeRegistryService.getConfigClassName(type);
        return configClassName.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 获取节点的示例配置
     * <p>
     * 返回指定节点类型的一个空配置对象示例。
     * 展示了该节点需要哪些配置字段，可用于前端表单生成。
     * </p>
     *
     * @param type 节点类型名称
     * @return 示例配置（JSON 格式的键值对），如果类型不存在返回 404
     */
    @GetMapping("/example-config/{type}")
    public ResponseEntity<Map<String, Object>> getExampleConfig(@PathVariable String type) {
        log.debug("Fetching example config for type={}", type);

        Optional<Map<String, Object>> exampleConfig = nodeRegistryService.getExampleConfig(type);
        return exampleConfig.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 检查节点类型是否已注册
     * <p>
     * 验证指定节点类型是否在注册表中存在。
     * 可用于工作流验证或动态节点创建前的检查。
     * </p>
     *
     * @param type 节点类型名称
     * @return 包含是否已注册的结果对象
     */
    @GetMapping("/check/{type}")
    public ResponseEntity<Map<String, Object>> checkTypeRegistered(@PathVariable String type) {
        log.debug("Checking if type={} is registered", type);

        boolean registered = nodeRegistryService.isTypeRegistered(type);

        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("registered", registered);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取已注册的节点数量
     * <p>
     * 返回当前注册表中的节点类型总数。
     * 可用于系统健康检查或统计信息。
     * </p>
     *
     * @return 包含节点数量的结果对象
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getRegisteredCount() {
        log.debug("Fetching registered node count");

        int count = nodeRegistryService.getRegisteredCount();

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);

        return ResponseEntity.ok(result);
    }
}
