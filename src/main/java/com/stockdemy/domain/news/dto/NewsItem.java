package com.stockdemy.domain.news.dto;

import lombok.Builder;

@Builder
public record NewsItem(
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
  String sourceName
) {
}
