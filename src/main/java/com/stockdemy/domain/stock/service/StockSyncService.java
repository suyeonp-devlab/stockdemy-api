package com.stockdemy.domain.stock.service;

import com.stockdemy.domain.stock.entity.PriceBar;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.PriceBarRepository;
import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.domain.stock.store.StockQuoteStore;
import com.stockdemy.infra.marketdata.MarketDataProvider;
import com.stockdemy.infra.marketdata.dto.BarItem;
import com.stockdemy.infra.marketdata.dto.QuoteItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockSyncService {

  private final StockRepository stockRepository;
  private final PriceBarRepository priceBarRepository;
  private final MarketDataProvider marketDataProvider;
  private final StockQuoteStore stockQuoteStore;

  // 종목 시세 조회 → Redis 저장
  public boolean syncQuote(String stockCode, String market) {

    Optional<QuoteItem> quote = marketDataProvider.fetchQuote(stockCode, market);

    if (quote.isEmpty()) {
      return false;
    }

    QuoteItem item = quote.get();

    try {
      stockQuoteStore.save(stockCode, StockQuoteStore.Snapshot.builder()
        .price(item.price())
        .previousClose(item.previousClose())
        .changePercent(item.changePercent())
        .volume(item.volume())
        .week52High(item.week52High())
        .week52Low(item.week52Low())
        .quotedAt(item.quotedAt())
        .build());
      return true;

    } catch (Exception e) {
      log.warn("시세 캐시 저장 실패 (stockCode={}): {}", stockCode, e.getMessage());
      return false;
    }
  }

  // Redis 시세 → DB 스냅샷 반영 (Redis에 값이 없는 종목은 기존 값 유지)
  @Transactional
  public int flushQuotes(List<String> stockCodes) {

    Map<String, StockQuoteStore.Snapshot> snapshots = stockQuoteStore.findAll(stockCodes);

    if (snapshots.isEmpty()) {
      return 0;
    }

    List<Stock> stocks = stockRepository.findAllById(snapshots.keySet());

    for (Stock stock : stocks) {

      StockQuoteStore.Snapshot snapshot = snapshots.get(stock.getStockCode());
      stock.applyQuote(
        snapshot.price(),
        snapshot.previousClose(),
        snapshot.changePercent(),
        snapshot.volume(),
        snapshot.week52High(),
        snapshot.week52Low()
      );
    }

    return stocks.size();
  }

  /**
   * 최근 일봉 중 누락된 날짜만 추가
   *
   * <p>이미 있는 날짜는 건너뛰므로 여러 번 실행해도 결과가 같다. 배치가 빠진 날도 다음 실행에서
   * 채워진다. 외부 호출 동안 DB 커넥션을 잡지 않도록 트랜잭션을 두지 않는다.
   */
  public int fillRecentBars(String stockCode, String market, int days) {

    List<BarItem> bars = marketDataProvider.fetchDailyBars(stockCode, market, days);

    if (bars.isEmpty()) {
      return 0;
    }

    LocalDate from = bars.getFirst().date();
    Set<LocalDate> existing = new HashSet<>(priceBarRepository.findBarDatesSince(stockCode, from));

    List<PriceBar> missing = bars.stream()
      .filter(bar -> !existing.contains(bar.date()))
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
      .toList();

    priceBarRepository.saveAll(missing);

    return missing.size();
  }
}
