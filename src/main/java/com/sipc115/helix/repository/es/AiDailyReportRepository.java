package com.sipc115.helix.repository.es;

import com.sipc115.helix.model.es.AiDailyReportDocument;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiDailyReportRepository extends ElasticsearchRepository<AiDailyReportDocument, String> {

    List<AiDailyReportDocument> findTop5BySummaryTextContainingOrDetailTextContainingOrderByCreatedAtDesc(
            String summaryKeyword, String detailKeyword);

    List<AiDailyReportDocument> findTop5ByReportDateOrderByCreatedAtDesc(LocalDate reportDate);

    boolean existsBySummaryTextContainingOrDetailTextContaining(String summaryKeyword, String detailKeyword);
}
