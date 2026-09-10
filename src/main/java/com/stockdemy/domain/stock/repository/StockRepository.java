package com.stockdemy.domain.stock.repository;

import com.stockdemy.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
