package com.stockdemy.domain.market.scheduler;

import com.stockdemy.domain.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 지수·환율 갱신
 *
 * <p>원/달러는 KRX·NASDAQ 장외 시간에도 움직여 장 시간과 무관하게 항상 실행한다 (1회 4건 호출).
 */
@Component
@RequiredArgsConstructor
public class MarketIndexSyncScheduler {

  private final MarketService marketService;

  @Scheduled(
    fixedDelayString = "${market-data.index-sync-interval-ms}",
    initialDelayString = "${market-data.index-sync-initial-delay-ms}"
  )
  public void syncIndices() {
    marketService.syncIndices();
  }
}
