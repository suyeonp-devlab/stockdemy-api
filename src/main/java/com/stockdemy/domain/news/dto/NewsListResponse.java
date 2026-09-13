package com.stockdemy.domain.news.dto;

import java.util.List;

public record NewsListResponse(
  long totalCount,
  int totalPages,
  List<NewsItem> items
) {
}
