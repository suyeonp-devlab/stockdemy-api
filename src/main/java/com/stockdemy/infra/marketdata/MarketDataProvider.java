package com.stockdemy.infra.marketdata;

import com.stockdemy.infra.marketdata.dto.BarItem;
import com.stockdemy.infra.marketdata.dto.IndexItem;
import com.stockdemy.infra.marketdata.dto.IntradayItem;
import com.stockdemy.infra.marketdata.dto.QuoteItem;

import java.util.List;
import java.util.Optional;

/**
 * 시세 조회 프로바이더
 *
 * <p>조회 실패 시 예외를 던지지 않고 비어 있는 결과를 반환한다. 폴백(마지막 동기화 시세 사용)은
 * 호출하는 도메인 서비스의 책임이다.
 */
public interface MarketDataProvider {

  /** 현재가 조회 */
  Optional<QuoteItem> fetchQuote(String stockCode, String market);

  /** 일봉 조회 (최근 {@code days}일) */
  List<BarItem> fetchDailyBars(String stockCode, String market, int days);

  /** 당일 시세 + 분봉 조회 (장 시작 전·휴장일에는 최근 거래일 기준) */
  Optional<IntradayItem> fetchIntraday(String stockCode, String market);

  /** 지수·환율 조회 */
  List<IndexItem> fetchIndices();
}
