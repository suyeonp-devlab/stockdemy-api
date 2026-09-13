package com.stockdemy.domain.news.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record NewsSearchRequest(
  String category,
  String keyword,
  Boolean favorite,

  @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
  Integer page,

  @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
  @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
  Integer pageSize
) {

  private static final int DEFAULT_PAGE_SIZE = 10;

  // 누락된 조건 기본값 처리
  public NewsSearchRequest {
    keyword = keyword == null ? "" : keyword.trim();
    favorite = Boolean.TRUE.equals(favorite);
    page = page == null ? 1 : page;
    pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
  }
}
