package com.stockdemy.domain.journal.dto;

import com.stockdemy.domain.journal.entity.JournalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record JournalSearchRequest(
  @Pattern(regexp = "^(STANDBY|PENDING|DONE)?$", message = "올바르지 않은 상태 값입니다.")
  String status,

  @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
  Integer page,

  @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
  @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
  Integer pageSize
) {

  private static final int DEFAULT_PAGE_SIZE = 10;

  // 누락된 조건 기본값 처리
  public JournalSearchRequest {
    status = status == null ? "" : status.trim();
    page = page == null ? 1 : page;
    pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
  }

  // 상태 조건 (빈 값이면 전체)
  public JournalStatus statusFilter() {
    return status.isEmpty() ? null : JournalStatus.valueOf(status);
  }
}
