package com.sipc115.helix.repository.jpa;

import com.sipc115.helix.domain.entity.ReportTask;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 报告任务仓库接口
 * <p>
 * 用于操作ReportTask实体的JPA仓库接口，提供基本的CRUD操作
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
public interface ReportTaskRepository extends JpaRepository<ReportTask, String> {
}
