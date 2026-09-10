package com.stockdemy.domain.stock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record TodayQuoteResponse(
  Double price,
  Double changePercent,
  PriceBarItem todayBar,
  MinuteBarItem latestMinuteBar,

  // boolean 접근자 규칙으로 "marketOpen"이 되지 않도록 앱 필드명을 명시
  @JsonProperty("isMarketOpen")
  boolean isMarketOpen,

  String updatedAt
) {
}
