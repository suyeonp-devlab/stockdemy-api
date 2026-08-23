package com.stockdemy.infra.ai.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record JournalReviewRequest(
  String stockCode,
  String marketName,
  String tradeTypeName,
  LocalDate tradeDate,
  LocalTime tradeTime,
  double price,
  int quantity,
  String memo) {
}
