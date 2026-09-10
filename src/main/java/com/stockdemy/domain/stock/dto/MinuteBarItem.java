package com.stockdemy.domain.stock.dto;

import com.stockdemy.infra.marketdata.dto.BarItem;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.stockdemy.global.util.NumberUtil.round2;

/** 분봉 (거래소 현지 시각, date = yyyy-MM-dd / time = HH:mm) */
@Builder
public record MinuteBarItem(
  String date,
  String time,
  Double open,
  Double high,
  Double low,
  Double close,
  Long volume,
  Long tradingValue
) {

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  public static MinuteBarItem from(BarItem bar) {
    return MinuteBarItem.builder()
      .date(bar.date().toString())
      .time(bar.time().format(TIME_FORMAT))
      .open(round2(bar.open()))
      .high(round2(bar.high()))
      .low(round2(bar.low()))
      .close(round2(bar.close()))
      .volume(bar.volume())
      .tradingValue(bar.tradingValue())
      .build();
  }

  // 분봉이 아직 없을 때의 자리표시 봉 (앱 차트가 최신 분봉을 항상 요구함)
  public static MinuteBarItem placeholder(LocalDate date, LocalTime time, double price) {
    return MinuteBarItem.builder()
      .date(date.toString())
      .time(time.format(TIME_FORMAT))
      .open(round2(price))
      .high(round2(price))
      .low(round2(price))
      .close(round2(price))
      .volume(0L)
      .tradingValue(0L)
      .build();
  }
}
