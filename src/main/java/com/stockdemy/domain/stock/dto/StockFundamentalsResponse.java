package com.stockdemy.domain.stock.dto;

import lombok.Builder;

/** 종목 기초데이터 (수집하지 않은 항목은 null → 응답에서 필드 누락) */
@Builder
public record StockFundamentalsResponse(
  String stockCode,
  String stockName,
  String market,
  String marketNm,
  String sector,
  String sectorNm,
  String sentiment,
  String sentimentNm,
  boolean favorite,
  String aiComment,
  Double prevClose,
  Double week52High,
  Double week52Low,
  Long sharesOutstanding,
  Double foreignOwnership,
  Double eps,
  Double bps,
  Double annualDividend,
  Double sectorPer
) {
}
