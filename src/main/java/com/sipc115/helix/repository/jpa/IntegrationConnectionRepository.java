/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.repository.jpa;

import com.sipc115.helix.domain.integration.IntegrationConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationConnectionRepository extends JpaRepository<IntegrationConnection, Long> {

    Optional<IntegrationConnection> findByName(String name);

    List<IntegrationConnection> findByType(String type);

    List<IntegrationConnection> findByTypeAndStatus(String type, String status);

    List<IntegrationConnection> findByCategory(String category);

    List<IntegrationConnection> findByStatus(String status);

    Optional<IntegrationConnection> findByTypeAndIsDefaultTrue(String type);

    boolean existsByName(String name);

    @Modifying
    @Query("UPDATE IntegrationConnection c SET c.isDefault = false WHERE c.type = :type AND c.isDefault = true")
    void clearDefaultForType(@Param("type") String type);

    List<IntegrationConnection> findByTypeOrderByCreatedAtDesc(String type);
}