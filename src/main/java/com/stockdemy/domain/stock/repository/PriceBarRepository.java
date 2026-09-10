package com.stockdemy.domain.stock.repository;

import com.stockdemy.domain.stock.entity.PriceBar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PriceBarRepository extends JpaRepository<PriceBar, Long> {

  boolean existsByStockCode(String stockCode);

  List<PriceBar> findByStockCodeOrderByBarDateAsc(String stockCode);

  // 기준일 이후 이미 적재된 봉 기준일
  @Query("SELECT p.barDate FROM PriceBar p WHERE p.stockCode = :stockCode AND p.barDate >= :from")
  List<LocalDate> findBarDatesSince(@Param("stockCode") String stockCode, @Param("from") LocalDate from);
}
