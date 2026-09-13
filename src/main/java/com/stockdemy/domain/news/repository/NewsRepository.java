package com.stockdemy.domain.news.repository;

import com.stockdemy.domain.news.entity.News;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {

  // 이미 수집한 기사 여부 (원문 URL 유니크)
  boolean existsBySourceUrl(String sourceUrl);

  // 해당 시각 이후 수집 이력 여부 (기동 시 중복 수집 방지)
  boolean existsByCreatedAtAfter(LocalDateTime from);

  // 종목별 최근 뉴스 발행 시각 (수집 우선순위 계산용)
  @Query("SELECT n.stockCode AS stockCode, MAX(n.publishedAt) AS publishedAt FROM News n GROUP BY n.stockCode")
  List<StockNewsTime> findLatestPublishedAtByStock();

  // 종목의 최근 뉴스 (주체 종목이거나 관련 종목으로 엮인 뉴스, 최신순)
  @Query("SELECT n FROM News n WHERE n.publishedAt >= :from AND (n.stockCode = :stockCode "
    + "OR n.newsId IN (SELECT r.newsId FROM NewsRelatedStock r WHERE r.stockCode = :stockCode)) "
    + "ORDER BY n.publishedAt DESC, n.newsId DESC")
  List<News> findRecentByStock(
    @Param("stockCode") String stockCode,
    @Param("from") LocalDateTime from,
    Pageable pageable
  );

  // 기간 내 종목 뉴스 (주체 종목이거나 관련 종목으로 엮인 뉴스, 최신순, 종료 시각 미포함)
  @Query("SELECT n FROM News n WHERE n.publishedAt >= :from AND n.publishedAt < :to AND (n.stockCode = :stockCode "
    + "OR n.newsId IN (SELECT r.newsId FROM NewsRelatedStock r WHERE r.stockCode = :stockCode)) "
    + "ORDER BY n.publishedAt DESC, n.newsId DESC")
  List<News> findByStockBetween(
    @Param("stockCode") String stockCode,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to,
    Pageable pageable
  );

  // 기간 내 감성별 뉴스 건수
  @Query("SELECT n.sentiment AS sentiment, COUNT(n) AS count FROM News n WHERE n.publishedAt >= :from GROUP BY n.sentiment")
  List<SentimentCount> countBySentimentSince(@Param("from") LocalDateTime from);

  // 기간 내 주체 종목별 뉴스 건수
  @Query("SELECT n.stockCode AS stockCode, COUNT(n) AS count FROM News n WHERE n.publishedAt >= :from GROUP BY n.stockCode")
  List<StockCount> countByStockSince(@Param("from") LocalDateTime from);

  // 기간 내 관련 종목별 언급 건수
  @Query("SELECT r.stockCode AS stockCode, COUNT(r) AS count FROM NewsRelatedStock r, News n "
    + "WHERE n.newsId = r.newsId AND n.publishedAt >= :from GROUP BY r.stockCode")
  List<StockCount> countRelatedByStockSince(@Param("from") LocalDateTime from);

  interface StockNewsTime {
    String getStockCode();

    LocalDateTime getPublishedAt();
  }

  interface SentimentCount {
    String getSentiment();

    Long getCount();
  }

  interface StockCount {
    String getStockCode();

    Long getCount();
  }
}
