package com.stockdemy.domain.stock.scheduler;

import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.domain.stock.service.StockSyncService;
import com.stockdemy.domain.stock.support.MarketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 장마감 처리
 *
 * <p>시장별 마감 후 종가를 다시 받아 Redis → DB로 반영하고, 최근 일봉 중 빠진 날짜를 채운다.
 * 휴장일에 실행돼도 새 데이터가 없어 결과가 바뀌지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketCloseScheduler {

  // 배치가 며칠 빠져도 다음 실행에서 채워지도록 여유를 둔 조회 기간
  private static final int RECENT_BAR_DAYS = 7;

  private final StockRepository stockRepository;
  private final StockSyncService stockSyncService;

  @Value("${market-data.call-interval-ms}")
  private long callIntervalMs;

  // KRX 장마감 처리
  @Scheduled(cron = "${market-data.close.krx-cron}", zone = "Asia/Seoul")
  public void closeKrx() {
    close(MarketSession.KOSPI, MarketSession.KOSDAQ);
  }

  // NASDAQ 장마감 처리
  @Scheduled(cron = "${market-data.close.nasdaq-cron}", zone = "America/New_York")
  public void closeNasdaq() {
    close(MarketSession.NASDAQ);
  }

  private void close(MarketSession... sessions) {

    for (MarketSession session : sessions) {

      String market = session.getMarket();
      List<String> stockCodes = stockRepository.findSyncedCodesByMarket(market);

      if (stockCodes.isEmpty()) continue;

      int addedBars = 0;

      for (String stockCode : stockCodes) {

        try {
          // 장중 마지막 갱신은 정규장 종료 전 값이므로 확정 종가를 한 번 더 받는다
          stockSyncService.syncQuote(stockCode, market);
          addedBars += stockSyncService.fillRecentBars(stockCode, market, RECENT_BAR_DAYS);
        } catch (Exception e) {
          log.warn("장마감 처리 실패 (stockCode={}): {}", stockCode, e.getMessage());
        }

        if (!pause()) return;
      }

      int flushed = stockSyncService.flushQuotes(stockCodes);
      log.info("장마감 처리 ({}): 시세 반영 {}/{}건, 일봉 추가 {}건", market, flushed, stockCodes.size(), addedBars);
    }
  }

  // 호출 간격 대기 (중단 요청 시 false)
  private boolean pause() {

    try {
      Thread.sleep(callIntervalMs);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("장마감 처리 중단");
      return false;
    }
  }
}
