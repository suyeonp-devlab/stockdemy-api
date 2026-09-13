package com.stockdemy.domain.stock.repository;

import com.stockdemy.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface StockRepository extends JpaRepository<Stock, String> {

  // 시세를 한 번도 받지 못한 종목 코드
  @Query("SELECT s.stockCode FROM Stock s WHERE s.lastSyncedAt IS NULL ORDER BY s.stockCode")
  List<String> findCodesNeverSynced();

  // 초기 적재가 끝난 시장별 종목 코드 (초기 적재 대상은 백필 러너가 전담)
  @Query("SELECT s.stockCode FROM Stock s WHERE s.market = :market AND s.lastSyncedAt IS NOT NULL ORDER BY s.stockCode")
  List<String> findSyncedCodesByMarket(@Param("market") String market);

  // 시장별 종목
  List<Stock> findByMarket(String market);

  // 해당 시각 이후 AI 분석 이력 여부 (기동 시 중복 분석 방지)
  boolean existsByAiAnalyzedAtAfter(LocalDateTime from);

  // AI 분석 결과 반영 (분석 배치는 외부 호출이 길어 엔티티를 트랜잭션에 묶지 않고 단건 갱신)
  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE Stock s SET s.sentiment = :sentiment, s.aiComment = :aiComment, "
    + "s.aiAnalyzedAt = :analyzedAt, s.updatedAt = :analyzedAt WHERE s.stockCode = :stockCode")
  int updateAnalysis(
    @Param("stockCode") String stockCode,
    @Param("sentiment") String sentiment,
    @Param("aiComment") String aiComment,
    @Param("analyzedAt") LocalDateTime analyzedAt
  );
}
