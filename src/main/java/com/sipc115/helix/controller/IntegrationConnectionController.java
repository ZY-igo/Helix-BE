/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.controller;

import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.service.integration.IntegrationConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integration/connections")
@RequiredArgsConstructor
public class IntegrationConnectionController {

    private final IntegrationConnectionService connectionService;

    @GetMapping
    public ResponseEntity<List<IntegrationConnection>> getAllConnections() {
        return ResponseEntity.ok(connectionService.getAllConnections());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IntegrationConnection> getConnection(@PathVariable Long id) {
        return connectionService.getConnection(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<IntegrationConnection>> getConnectionsByType(@PathVariable String type) {
        return ResponseEntity.ok(connectionService.getConnectionsByType(type));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<IntegrationConnection>> getConnectionsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(connectionService.getConnectionsByCategory(category));
    }

    @PostMapping
    public ResponseEntity<IntegrationConnection> createConnection(
            @RequestBody IntegrationConnectionRequest request) {
        IntegrationConnection created = connectionService.createConnection(request);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IntegrationConnection> updateConnection(
            @PathVariable Long id,
            @RequestBody IntegrationConnectionRequest request) {
        return connectionService.updateConnection(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
        connectionService.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<ConnectionTestResult> testConnection(
            @PathVariable Long id) {
        ConnectionTestResult result = connectionService.testConnection(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/set-default")
    public ResponseEntity<IntegrationConnection> setDefaultConnection(@PathVariable Long id) {
        return connectionService.setDefaultConnection(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}