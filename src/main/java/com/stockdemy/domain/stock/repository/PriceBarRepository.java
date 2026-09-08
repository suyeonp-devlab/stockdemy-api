package com.stockdemy.domain.stock.repository;

import com.stockdemy.domain.stock.entity.PriceBar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceBarRepository extends JpaRepository<PriceBar, Long> {

  boolean existsByStockCode(String stockCode);
}
