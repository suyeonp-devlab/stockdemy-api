package com.stockdemy.domain.stock.dto;

import java.util.List;

public record StockListResponse(
  int totalCount,
  int totalPages,
  List<StockItem> items
) {
}
