package com.stockdemy.domain.news.dto;

public record MentionRankingItem(
  String stockCode,
  String stockName,
  long count
) {
}
