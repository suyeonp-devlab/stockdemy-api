package com.stockdemy.infra.marketdata.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record QuoteItem(
  String stockCode,
  double price,
  double previousClose,
  double changePercent,
  long volume,
  double week52High,
  double week52Low,
  boolean marketOpen,
  LocalDateTime quotedAt) {
}
