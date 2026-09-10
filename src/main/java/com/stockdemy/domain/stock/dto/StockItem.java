package com.stockdemy.domain.stock.dto;

import lombok.Builder;

@Builder
public record StockItem(
  String stockCode,
  String stockName,
  String market,
  String marketNm,
  String sector,
  String sectorNm,
  Double price,
  Double changePercent,
  Long marketCap,
  String sentiment,
  String sentimentNm,
  boolean favorite
) {
}
