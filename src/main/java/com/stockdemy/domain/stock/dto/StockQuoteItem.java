package com.stockdemy.domain.stock.dto;

import lombok.Builder;

@Builder
public record StockQuoteItem(
  String stockCode,
  Double price,
  Double changePercent,
  Long marketCap
) {
}
