package com.stockdemy.domain.news.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record NewsDetailResponse(
  Long id,
  String stockCode,
  String stockName,
  String title,
  String summary,
  String publishedAt,
  String category,
  String categoryNm,
  String sentiment,
  String sentimentNm,
  String sourceUrl,
  String sourceName,
  Integer confidence,
  String reasoning,
  List<RelatedStockItem> relatedStocks
) {
}
