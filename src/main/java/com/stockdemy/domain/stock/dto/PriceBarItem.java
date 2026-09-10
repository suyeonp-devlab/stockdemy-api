package com.stockdemy.domain.stock.dto;

import com.stockdemy.domain.stock.entity.PriceBar;
import lombok.Builder;

import java.time.LocalDate;

import static com.stockdemy.global.util.NumberUtil.round2;

/** 일봉 (차트 라이브러리 형식, date = yyyy-MM-dd 거래소 현지 날짜) */
@Builder
public record PriceBarItem(
  String date,
  Double open,
  Double high,
  Double low,
  Double close,
  Long volume,
  Long tradingValue
) {

  public static PriceBarItem from(PriceBar bar) {
    return PriceBarItem.builder()
      .date(bar.getBarDate().toString())
      .open(round2(bar.getOpen()))
      .high(round2(bar.getHigh()))
      .low(round2(bar.getLow()))
      .close(round2(bar.getClose()))
      .volume(bar.getVolume())
      .tradingValue(bar.getTradingValue())
      .build();
  }

  // 당일 진행 중인 봉
  public static PriceBarItem of(LocalDate date, double open, double high, double low, double close, long volume) {
    return PriceBarItem.builder()
      .date(date.toString())
      .open(round2(open))
      .high(round2(high))
      .low(round2(low))
      .close(round2(close))
      .volume(volume)
      // Yahoo는 거래대금을 주지 않아 과거 일봉과 같은 기준(종가 × 거래량)으로 근사한다
      .tradingValue(Math.round(close * volume))
      .build();
  }
}
