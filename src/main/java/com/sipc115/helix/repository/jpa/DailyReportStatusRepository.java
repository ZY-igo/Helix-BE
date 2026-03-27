package com.sipc115.helix.repository.jpa;

import com.sipc115.helix.model.entity.DailyReportStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyReportStatusRepository extends JpaRepository<DailyReportStatus, Long> {
    Optional<DailyReportStatus> findByReportDate(LocalDate reportDate);
}
