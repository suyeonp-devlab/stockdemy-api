package com.stockdemy.infra.ai.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 일지 복기 입력 (없는 시장 자료는 null → 프롬프트에서 "정보 없음") */
@Builder
public record JournalReviewRequest(
  String stockCode,
  String stockName,
  String marketName,
  String tradeTypeName,
  LocalDate tradeDate,
  LocalTime tradeTime,
  double price,
  int quantity,
  String memo,
  TradeDayBar tradeDayBar,
  Double currentPrice,
  Double changeSinceTrade,
  List<StockNewsDigest> nearbyNews) {
}
