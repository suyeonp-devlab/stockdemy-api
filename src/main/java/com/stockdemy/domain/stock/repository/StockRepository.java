package com.stockdemy.domain.stock.repository;

import com.stockdemy.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, String> {

  // 시세를 한 번도 받지 못한 종목 코드
  @Query("SELECT s.stockCode FROM Stock s WHERE s.lastSyncedAt IS NULL ORDER BY s.stockCode")
  List<String> findCodesNeverSynced();
}
