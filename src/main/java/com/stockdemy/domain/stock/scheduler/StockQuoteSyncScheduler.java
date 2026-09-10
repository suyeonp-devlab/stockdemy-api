package com.stockdemy.domain.stock.scheduler;

import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.domain.stock.service.StockSyncService;
import com.stockdemy.domain.stock.support.MarketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 장중 시세 갱신
 *
 * <p>정규장이 열린 시장의 종목만 Yahoo에서 받아 Redis에 저장한다. DB 반영은 장마감 배치가 한다.
 * 사이클이 겹치지 않도록 이전 사이클 종료 후 대기한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockQuoteSyncScheduler {

  private final StockRepository stockRepository;
  private final StockSyncService stockSyncService;

  @Value("${market-data.call-interval-ms}")
  private long callIntervalMs;

  @Scheduled(
    fixedDelayString = "${market-data.sync-interval-ms}",
    initialDelayString = "${market-data.sync-initial-delay-ms}"
  )
  public void syncOpenMarkets() {

    Instant now = Instant.now();

    for (MarketSession session : MarketSession.values()) {

      if (!session.isOpen(now)) continue;

      List<String> stockCodes = stockRepository.findSyncedCodesByMarket(session.getMarket());
      int success = 0;

      for (String stockCode : stockCodes) {

        if (stockSyncService.syncQuote(stockCode, session.getMarket())) {
          success++;
        }

        if (!pause()) return;
      }

      log.debug("장중 시세 갱신 ({}): {}/{}건", session.getMarket(), success, stockCodes.size());
    }
  }

  // 호출 간격 대기 (중단 요청 시 false)
  private boolean pause() {

    try {
      Thread.sleep(callIntervalMs);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("장중 시세 갱신 중단");
      return false;
    }
  }
}
