package com.sipc115.helix.repository.es;

import com.sipc115.helix.domain.es.AiDailyReportDocument;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * AI日报报告仓库接口
 * <p>
 * 用于操作AiDailyReportDocument文档的Elasticsearch仓库接口，提供日报相关的查询方法
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Repository
public interface AiDailyReportRepository extends ElasticsearchRepository<AiDailyReportDocument, String> {

    /**
     * 根据摘要文本或详细文本中的关键词查询最新的5条日报
     * 
     * @param summaryKeyword 摘要关键词
     * @param detailKeyword 详细文本关键词
     * @return 匹配的日报文档列表
     */
    List<AiDailyReportDocument> findTop5BySummaryTextContainingOrDetailTextContainingOrderByCreatedAtDesc(
            String summaryKeyword, String detailKeyword);

    /**
     * 根据报告日期查询最新的5条日报
     * 
     * @param reportDate 报告日期
     * @return 匹配的日报文档列表
     */
    List<AiDailyReportDocument> findTop5ByReportDateOrderByCreatedAtDesc(LocalDate reportDate);

    /**
     * 检查是否存在包含指定关键词的日报
     * 
     * @param summaryKeyword 摘要关键词
     * @param detailKeyword 详细文本关键词
     * @return 是否存在匹配的日报
     */
    boolean existsBySummaryTextContainingOrDetailTextContaining(String summaryKeyword, String detailKeyword);
}
