/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.repository.jpa;

import com.sipc115.helix.domain.integration.IntegrationConnectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegrationConnectionHistoryRepository extends JpaRepository<IntegrationConnectionHistory, Long> {

    List<IntegrationConnectionHistory> findByConnectionIdOrderByChangedAtDesc(Long connectionId);
}