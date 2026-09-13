package com.stockdemy.infra.ai.dto;

import java.time.LocalDate;

/** 복기 입력용 거래일 일봉 (휴장일 거래는 직전 거래일 봉) */
public record TradeDayBar(
  LocalDate date,
  Double open,
  Double high,
  Double low,
  Double close) {
}
