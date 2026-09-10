package com.stockdemy.domain.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(
  @NotBlank(message = "종목 코드를 입력해주세요.")
  String stockCode,

  @NotNull(message = "관심종목 여부를 입력해주세요.")
  Boolean favorite
) {
}
