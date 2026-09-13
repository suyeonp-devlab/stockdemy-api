package com.stockdemy.domain.journal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

// 앱이 함께 보내는 종목명·시장·업종은 조작될 수 있어 받지 않고, 서버가 종목 마스터로 확정한다
@JsonIgnoreProperties(ignoreUnknown = true)
public record JournalSaveRequest(
  @NotBlank(message = "종목을 선택해주세요.")
  String stockCode,

  @NotBlank(message = "거래 유형을 선택해주세요.")
  @Pattern(regexp = "^(BUY|SELL)$", message = "올바르지 않은 거래 유형입니다.")
  String tradeType,

  @NotNull(message = "거래일을 선택해주세요.")
  @PastOrPresent(message = "거래일은 오늘 이후로 선택할 수 없습니다.")
  LocalDate tradeDate,

  LocalTime tradeTime,

  @NotNull(message = "거래 가격을 입력해주세요.")
  @Positive(message = "거래 가격은 0보다 커야 합니다.")
  Double price,

  @NotNull(message = "거래 수량을 입력해주세요.")
  @Min(value = 1, message = "거래 수량은 1 이상이어야 합니다.")
  Integer quantity,

  @Size(max = 1000, message = "메모는 1,000자 이하로 입력해주세요.")
  String memo
) {
}
