package com.stockdemy.infra.news.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NewsArticleItem(
  String title,
  String sourceUrl,
  String sourceName,
  LocalDateTime publishedAt) {
}
