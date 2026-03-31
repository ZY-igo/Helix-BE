package com.sipc115.helix.repository.jpa;

import com.sipc115.helix.domain.entity.DailyReportStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 日报状态仓库接口
 * <p>
 * 用于操作DailyReportStatus实体的JPA仓库接口，提供日报状态相关的查询方法
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
public interface DailyReportStatusRepository extends JpaRepository<DailyReportStatus, Long> {
    /**
     * 根据任务ID和报告日期查询日报状态
     * 
     * @param taskId 任务ID
     * @param reportDate 报告日期
     * @return 日报状态对象，可能为空
     */
    Optional<DailyReportStatus> findByTaskIdAndReportDate(String taskId, LocalDate reportDate);
}
