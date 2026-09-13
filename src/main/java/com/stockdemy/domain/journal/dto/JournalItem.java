package com.stockdemy.domain.journal.dto;

import lombok.Builder;

@Builder
public record JournalItem(
  Long id,
  String stockCode,
  String stockName,
  String status,
  String statusNm,
  String market,
  String marketNm,
  String sector,
  String sectorNm,
  String tradeType,
  String tradeTypeNm,
  String tradeDate,
  String tradeTime,
  Double price,
  Integer quantity,
  String memo,
  String aiComment,
  String createdAt
) {
}
