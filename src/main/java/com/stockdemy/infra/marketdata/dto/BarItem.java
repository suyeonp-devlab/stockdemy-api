package com.stockdemy.infra.marketdata.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 시세 봉 (일봉/분봉 공용)
 *
 * <p>일봉은 {@code time}이 null이다.
 */
@Builder
public record BarItem(
  LocalDate date,
  LocalTime time,
  double open,
  double high,
  double low,
  double close,
  long volume,
  long tradingValue) {
}
