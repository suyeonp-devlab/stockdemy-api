package com.stockdemy.domain.stock.service;

import com.stockdemy.domain.stock.entity.PriceBar;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.PriceBarRepository;
import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.domain.stock.support.MarketSession;
import com.stockdemy.infra.marketdata.MarketDataProvider;
import com.stockdemy.infra.marketdata.dto.BarItem;
import com.stockdemy.infra.marketdata.dto.QuoteItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockBackfillService {

  private final StockRepository stockRepository;
  private final PriceBarRepository priceBarRepository;
  private final MarketDataProvider marketDataProvider;

  /**
   * 종목 1건의 시세·일봉 초기 적재
   *
   * <p>종목 단위로 트랜잭션을 끊어 일부 실패가 전체를 되돌리지 않게 한다. 실패한 종목은
   * {@code lastSyncedAt}이 비어 있는 상태로 남아 다음 기동 때 재시도된다.
   */
  @Transactional
  public boolean backfill(String stockCode, int days) {

    Optional<Stock> found = stockRepository.findById(stockCode);

    if (found.isEmpty()) {
      log.warn("종목을 찾을 수 없습니다 (stockCode={})", stockCode);
      return false;
    }

    Stock stock = found.get();
    Optional<QuoteItem> quote = marketDataProvider.fetchQuote(stockCode, stock.getMarket());

    if (quote.isEmpty()) {
      log.warn("시세 조회 결과 없음 (stockCode={})", stockCode);
      return false;
    }

    QuoteItem item = quote.get();
    stock.applyQuote(
      item.price(),
      item.previousClose(),
      item.changePercent(),
      item.volume(),
      item.week52High(),
      item.week52Low()
    );

    saveDailyBars(stockCode, stock.getMarket(), days);

    return true;
  }

  // 일봉 적재 (이미 적재된 종목은 건너뜀)
  private void saveDailyBars(String stockCode, String market, int days) {

    if (priceBarRepository.existsByStockCode(stockCode)) {
      return;
    }

    Optional<MarketSession> session = MarketSession.of(market);

    if (session.isEmpty()) {
      log.warn("장 운영 정보가 없는 시장입니다 (stockCode={}, market={})", stockCode, market);
      return;
    }

    // 종가 확정 전 봉은 이후 보정되지 않으므로 제외한다 (해당 봉은 장마감 배치가 적재)
    Instant now = Instant.now();

    List<BarItem> bars = marketDataProvider.fetchDailyBars(stockCode, market, days).stream()
      .filter(bar -> session.get().isSettled(bar.date(), now))
      .toList();

    if (bars.isEmpty()) {
      log.warn("일봉 조회 결과 없음 (stockCode={})", stockCode);
      return;
    }

    priceBarRepository.saveAll(bars.stream()
      .map(bar -> PriceBar.builder()
        .stockCode(stockCode)
        .barDate(bar.date())
        .open(bar.open())
        .high(bar.high())
        .low(bar.low())
        .close(bar.close())
        .volume(bar.volume())
        .tradingValue(bar.tradingValue())
        .build())
      .toList());
  }
}
