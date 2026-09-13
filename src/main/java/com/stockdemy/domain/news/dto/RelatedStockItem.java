package com.stockdemy.domain.news.dto;

import lombok.Builder;

@Builder
public record RelatedStockItem(
  String stockCode,
  String stockName,
  String impact,
  String impactNm
) {
}
