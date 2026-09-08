package com.stockdemy.infra.marketdata;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대시보드 지수·환율 정의
 *
 * <p>{@code dummyBaseValue}는 local 프로바이더 전용 기준값이다. 실 시세는 Yahoo에서 받아온다.
 */
@Getter
@RequiredArgsConstructor
public enum MarketIndexType {

  KOSPI("KOSPI", "코스피", "^KS11", 2789.42),
  KOSDAQ("KOSDAQ", "코스닥", "^KQ11", 845.13),
  NASDAQ("NASDAQ", "나스닥", "^IXIC", 21450.12),
  USDKRW("USDKRW", "원/달러", "KRW=X", 1382.5);

  private final String marketCode;
  private final String marketName;
  private final String yahooSymbol;
  private final double dummyBaseValue;
}
