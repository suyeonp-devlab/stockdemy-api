package com.stockdemy.domain.news.dto;

public record SentimentSummaryResponse(
  int positive,
  int neutral,
  int negative,
  long totalCount
) {
}
