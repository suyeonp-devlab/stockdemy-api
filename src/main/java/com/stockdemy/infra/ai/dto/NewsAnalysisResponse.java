package com.stockdemy.infra.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record NewsAnalysisResponse(
  String summary,
  String sentiment,
  int confidence,
  String reasoning,
  List<NewsRelatedStockItem> relatedStocks) {
}
