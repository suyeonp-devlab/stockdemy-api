package com.stockdemy.domain.stock.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StockSearchRequest(
  String market,
  String sector,
  String keyword,
  Boolean favorite,
  Boolean topVolume,

  @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
  Integer page,

  @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
  @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
  Integer pageSize
) {

  private static final int DEFAULT_PAGE_SIZE = 15;

  // 누락된 조건 기본값 처리
  public StockSearchRequest {
    keyword = keyword == null ? "" : keyword.trim();
    favorite = Boolean.TRUE.equals(favorite);
    topVolume = Boolean.TRUE.equals(topVolume);
    page = page == null ? 1 : page;
    pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
  }
}
